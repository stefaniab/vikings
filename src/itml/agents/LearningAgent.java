package itml.agents;

import itml.cards.*;
import itml.simulator.CardDeck;
import itml.simulator.StateAgent;
import itml.simulator.StateBattle;
import weka.classifiers.Classifier;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.pmml.consumer.NeuralNetwork;
import weka.classifiers.trees.J48;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;

/**
 * User: deong
 * Date: 9/28/14
 */
public class LearningAgent extends Agent {
	private int m_noThisAgent;     // Index of our agent (0 or 1).
	private int m_noOpponentAgent; // Inex of opponent's agent.
	private Classifier classifier_;
	
	Instances myInstances;

	public LearningAgent( CardDeck deck, int msConstruct, int msPerMove, int msLearn ) {
		super(deck, msConstruct, msPerMove, msLearn);
		classifier_ = new MultilayerPerceptron();
	}

	@Override
	public void startGame(int noThisAgent, StateBattle stateBattle) {
		// Remember the indicies of the agents in the StateBattle.
		m_noThisAgent = noThisAgent;
		m_noOpponentAgent  = (noThisAgent == 0 ) ? 1 : 0; // can assume only 2 agents battling.
	}

	@Override
	public void endGame(StateBattle stateBattle, double[] results) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Card act(StateBattle stateBattle) {
		double[] values = new double[8];
		StateAgent a = stateBattle.getAgentState(0);
		StateAgent o = stateBattle.getAgentState(1);
		values[0] = a.getCol();
		values[1] = a.getRow();
		values[2] = a.getHealthPoints();
		values[3] = a.getStaminaPoints();
		values[4] = o.getCol();
		values[5] = o.getRow();
		values[6] = o.getHealthPoints();
		values[7] = o.getStaminaPoints();
		try {
			ArrayList<Card> allCards = m_deck.getCards();
			ArrayList<Card> cards = m_deck.getCards(a.getStaminaPoints());
			
			Instance currentInstance = new Instance(1.0, values.clone());
			currentInstance.setDataset(myInstances);
			
			int out = (int)classifier_.classifyInstance(currentInstance);
			Card selected = allCards.get(out);
			if(cards.contains(selected)) {
				//System.out.println("Predicted opponent card: " + selected.getName());
				//return selected;
				Card ourCard = getMove(stateBattle, selected);
				//System.out.println("Our card: " + ourCard.getName());
				return ourCard;
			}
		} catch (Exception e) {
			System.out.println("Error classifying new instance: " + e.toString());
		}
		return new CardRest();  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Classifier learn(Instances instances) {
		instances.setClassIndex(instances.numAttributes() - 1);
		myInstances = instances;
		try {
			classifier_.buildClassifier(instances);
		} catch(Exception e) {
			System.out.println("Error training classifier: " + e.toString());
		}
		//System.out.println(classifier_);
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}
	
	private Card getMove(StateBattle stateBattle, Card opponentCard)
	{
		StateAgent a = stateBattle.getAgentState(0);
		StateAgent o = stateBattle.getAgentState(1);
		
		int manhattan = Math.abs(o.getCol() + opponentCard.getCol() - a.getCol()) + Math.abs(o.getRow() + opponentCard.getRow() - a.getRow());
		
		if (a.getStaminaPoints() == 0) return new CardRest();
		
		if (manhattan > 3) 
		{
			if (a.getStaminaPoints() < 5 ) return new CardRest(); 
			if (o.getCol() + opponentCard.getCol() == a.getCol()) 
			{
				if (o.getRow() + opponentCard.getRow() < a.getRow()) return new CardMoveDown();
				else return new CardMoveUp();
			}
			if (o.getRow() + opponentCard.getRow() == a.getRow()) 
			{
				if (o.getCol() + opponentCard.getCol() < a.getCol()) return new CardMoveLeft();
				else return new CardMoveRight();
			}
		}
		
		if (o.getHealthPoints() <= a.getHealthPoints()) 
		{
			// attack moves
			if (manhattan < 2) return new CardAttackCardinal();
			if (manhattan == 2)
			{
				if (o.getRow() + opponentCard.getRow() == a.getRow()) return new CardAttackLong(); 
				if (o.getCol() + opponentCard.getCol() != a.getCol()) return new CardAttackDiagonal();
			}
		}
		
		
		return new CardRest();
	}
}
