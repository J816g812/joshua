/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */
package joshua.decoder.ff.tm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.FeatureFunction;

/**
 * this class implements MonolingualRule
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */


public class MonolingualRule implements Rule {
	
//===============================================================
// Instance Fields
//===============================================================
	
	/* The string format of Rule is:
	 * [Phrase] ||| french ||| english ||| feature scores
	 */
	private int rule_id;
	private int lhs; // tag of this rule
	private int[] p_french; //pointer to the RuleCollection, as all the rules under it share the same Source side
	private int arity;
	private float[] feat_scores; // the feature scores for this rule
	
	/* a feature function will be fired for this rule
	 * only if the owner of the rule matches the owner of the feature function
	 */
	private int owner;
	
	// TODO: consider remove this from the general class, and
	// create a new specific Rule class
	private	float lattice_cost; 
	
	/**
	 * estimate_cost depends on rule itself: statelesscost +
	 * transition_cost(non-stateless/non-contexual* models),
	 * we need this variable in order to provide sorting for
	 * cube-pruning
	 */
	private float est_cost = 0;

//===============================================================
// Static Fields
//===============================================================
	
	// TODO: Ideally, we shouldn't have to have dummy rule IDs
	// and dummy owners. How can this need be eliminated?
	private static final int DUMMY_RULE_ID = 1;
	private static final int DUMMY_OWNER = 1;
	
	
//===============================================================
// Constructors
//===============================================================
	
	/**
	 * Constructs a new rule using the provided parameters. The
	 * owner and rule id for this rule are undefined.
	 * 
	 * @param lhs Left-hand side of the rule.
	 * @param source_rhs Source language right-hand side of the rule.
	 * @param target_rhs Target language right-hand side of the rule.
	 * @param feature_scores Feature value scores for the rule.
	 * @param arity Number of nonterminals in the source language
	 *              right-hand side.
	 */
	public MonolingualRule(int lhs_, int[] source_rhs, float[] feature_scores, int arity_, int owner_, float lattice_cost_, int rule_id_) {
		this.lhs          = lhs_;
		this.p_french     = source_rhs;
		this.feat_scores  = feature_scores;
		this.arity        = arity_;
		this.lattice_cost = lattice_cost_;
		this.rule_id      = rule_id_;
		this.owner        = owner_;
	}

	
	// called by class who does not care about lattice_cost,
	// rule_id, and owner
	public MonolingualRule(int lhs_, int[] source_rhs, float[] feature_scores, int arity_) {
		this.lhs         = lhs_;
		this.p_french    = source_rhs;
		this.feat_scores = feature_scores;
		this.arity       = arity_;
		
		//==== dummy values
		this.lattice_cost = 0;
		this.rule_id      = DUMMY_RULE_ID;
		this.owner        = DUMMY_OWNER;
	}
	
	
//===============================================================
// Attributes
//===============================================================
	
	public final void setRuleID(int id) { this.rule_id = id; }
	
	public final int getRuleID() { return this.rule_id; }
	
	
	public final void setArity(int arity) { this.arity = arity; }
	
	public final int getArity() { return this.arity; }
	
	
	public final void setOwner(int owner) { this.owner = owner; }
	
	public final int getOwner() { return this.owner; }
	
	
	public final void setLHS(int lhs) { this.lhs = lhs; }
	
	public final int getLHS() { return this.lhs; }
	
	
	public void setEnglish(int[] eng) {
		//TODO: do nothing
	}
	
	public int[] getEnglish() {
		//TODO
		return null;
	}
	
	
	public final void setFrench(int[] french) { this.p_french = french; }
	
	public final int[] getFrench() { return this.p_french; }
	
	
	public final void setFeatureScores(float[] scores) {
		this.feat_scores = scores;
	}
	
	public final float[] getFeatureScores() {
		return this.feat_scores;
	}
	
	
	public final void setLatticeCost(float cost) { this.lattice_cost = cost; }
	
	public final float getLatticeCost() { return this.lattice_cost; }
	
	
	public final float getEstCost() {
		if (est_cost <= Double.NEGATIVE_INFINITY) {
			System.out.println("The est cost is neg infinity; must be bad rule; rule is:\n" + toString());
		}
		return est_cost;
	}
	
	
	/** 
	 * Set a lower-bound estimate inside the rule returns full
	 * estimate.
	 */
	public final float estimateRuleCost(ArrayList<FeatureFunction> featureFunctions) {
		if (null == featureFunctions) {
			return 0;
		} else {
			float estcost = 0.0f;
			for (FeatureFunction ff : featureFunctions) {
				double mdcost = ff.estimate(this) * ff.getWeight();
				estcost += mdcost;
			}
			this.est_cost = estcost;
			return estcost;
		}
	}
	
//===============================================================
// Methods
//===============================================================
	
	public float incrementFeatureScore(int column, double score) {
		synchronized(this) {
			feat_scores[column] += score;
			return feat_scores[column];
		}
	}
	
	
	public void setFeatureScore(int column, float score) {
		synchronized(this) {
			feat_scores[column] = score;
		}
	}
	
	
	public float getFeatureScore(int column) {
		synchronized(this) {
			return feat_scores[column];
		}
	}
	
	//===============================================================
	// Serialization Methods
	//===============================================================
		// BUG: These are all far too redundant. Should be refactored to share.
		
		// Caching this method significantly improves performance
		// We mark it transient because it is, though cf
		// java.io.Serializable
		private transient String cachedToString = null;
		
		@Deprecated
		public String toString(Map<Integer,String> ntVocab, SymbolTable sourceVocab, SymbolTable targetVocab) {
			if (null == this.cachedToString) {
				StringBuffer sb = new StringBuffer("[");
				sb.append(ntVocab.get(this.lhs));
				sb.append("] ||| ");
				sb.append(sourceVocab.getWords(this.p_french,true));
				sb.append(" |||");
				for (int i = 0; i < this.feat_scores.length; i++) {
					//sb.append(String.format(" %.4f", this.feat_scores[i]));
					sb.append(' ').append(Float.toString(this.feat_scores[i]));
				}
				this.cachedToString = sb.toString();
			}
			return this.cachedToString;
		}
		
		
		//print the rule in terms of Ingeters
		@Deprecated
		public String toString() {
			if (null == this.cachedToString) {
				StringBuffer sb = new StringBuffer("[");
				sb.append(this.lhs);
				sb.append("] ||| ");
				sb.append(Arrays.toString(this.p_french));
				sb.append(" |||");
				for (int i = 0; i < this.feat_scores.length; i++) {
					sb.append(String.format(" %.4f", this.feat_scores[i]));
				}
				this.cachedToString = sb.toString();
			}
			return this.cachedToString;
		}
		
		
		//do not use cachedToString
		@Deprecated
		public String toString(SymbolTable symbolTable) {
			StringBuffer sb = new StringBuffer("[");
			sb.append(symbolTable.getWord(this.lhs));
			sb.append("] ||| ");
			sb.append(symbolTable.getWords(this.p_french));
			sb.append(" |||");
			for (int i = 0; i < this.feat_scores.length; i++) {
				sb.append(String.format(" %.4f", this.feat_scores[i]));
			}
			return sb.toString();
		}
		
		@Deprecated
		public String toStringWithoutFeatScores(SymbolTable symbolTable) {
			return new StringBuffer("[")
				.append(symbolTable.getWord(lhs))
				.append("] ||| ")
				.append(symbolTable.getWords(p_french))
				.toString();
		}
}
