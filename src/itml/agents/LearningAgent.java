package itml.agents;

import itml.cards.*;
import itml.simulator.CardDeck;
import itml.simulator.StateAgent;
import itml.simulator.StateBattle;
import weka.classifiers.Classifier;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.trees.J48;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.Random;

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
		classifier_ = new J48();
		//classifier_ = new MultilayerPerceptron();
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
				//return selected;
				Card ourCard = getMove(stateBattle, selected);
				System.out.print("Predicted opponent card: " + selected.getName() + " ");
				System.out.println("Our card: " + ourCard.getName());
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
		System.out.println(classifier_);
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}
	
	private Card getMove(StateBattle stateBattle, Card opponentCard)
	{
		
		Random random = new Random();
		StateAgent a = stateBattle.getAgentState(m_noThisAgent);
		StateAgent o = stateBattle.getAgentState(m_noOpponentAgent);

		drawBoard(a, o);
		
		int manhattan = Math.abs(o.getCol() + opponentCard.getCol() - a.getCol()) + Math.abs(o.getRow() + opponentCard.getRow() - a.getRow());
		int currentManhattan = Math.abs(o.getCol() - a.getCol()) + Math.abs(o.getRow() - a.getRow());
		
		System.out.print("BATTLESTATE: health/stamina a: " + a.getHealthPoints() + " " + a.getStaminaPoints() + " o: "+ o.getHealthPoints() + " " + o.getStaminaPoints() + " ");
		System.out.print("Location: " + a.getRow() + " " + a.getCol() + " " + o.getRow() + " " + o.getCol() + " ");
		
		System.out.println("current manhattan : " + currentManhattan + " predicted manhattan : " + manhattan);
		
		// Resting
		if (a.getStaminaPoints() == 0 || a.getStaminaPoints() < 4 && manhattan > 3) return new CardRest();
		
		// Special case
		if (o.getStaminaPoints() == 0 && a.getStaminaPoints() > 1)
		{
			if (currentManhattan == 0) return new CardAttackCardinal();
			if (currentManhattan == 1) return new CardAttackDiagonal();
			if (currentManhattan == 2 && a.getRow() == o.getRow()) return new CardAttackLong();
		}
		
		// Aggressive
		if (a.getHealthPoints() > o.getHealthPoints())
		{
			System.out.println("AGGRESSIVE");
			// attack if in range
			if (a.getStaminaPoints() > 1)
			{
				if (manhattan < 2) return new CardAttackCardinal();
				if (manhattan == 2)
				{
					if (o.getRow() + opponentCard.getRow() == a.getRow()) return new CardAttackLong(); 
					if (o.getCol() + opponentCard.getCol() != a.getCol()) return new CardAttackDiagonal();
				}
			}
			// else move closer
			if (o.getCol() + opponentCard.getCol() == a.getCol()) 
			{
				if (o.getRow() + opponentCard.getRow() < a.getRow()) return new CardMoveDown();
				else return new CardMoveUp();
			}
			else if (o.getRow() + opponentCard.getRow() == a.getRow()) 
			{
				if (o.getCol() + opponentCard.getCol() < a.getCol()) return new CardMoveLeft();
				else return new CardMoveRight();
			}
			// vertical move
			if (random.nextBoolean())
			{
				if (a.getRow() > o.getRow()) return new CardMoveDown();
				else return new CardMoveUp();
			}
			else
			{
				if (a.getCol() > o.getCol()) 
				{
					if (a.getCol() > 1 && random.nextBoolean()) return new CardLeapLeft();
					return new CardMoveLeft();
				}
				if (a.getCol() < 3 && random.nextBoolean()) return new CardLeapRight();
				else return new CardMoveRight();
			}
			
		}
		// Defensive
		else if (a.getHealthPoints() < o.getHealthPoints())
		{
			System.out.println("DEFENSIVE");
			if (currentManhattan == 0) 
			{
				if (opponentCard.getName() == "cAttackCardinal") 
				{
					// leap action
					if (a.getCol() < 2) return new CardLeapRight();
					if (a.getCol() > 2) return new CardLeapLeft();
					if (random.nextBoolean()) return new CardLeapRight();
					else return new CardLeapLeft();
				}
				else if (opponentCard.getName() == "cAttackLong")
				{
					// move up/down
					if (a.getRow() == 0) return new CardMoveUp();
					if (a.getRow() == 4) return new CardMoveDown();
					if (random.nextBoolean()) return new CardMoveUp();
					else return new CardMoveDown();
				}
				else if (opponentCard.getName() == "cAttackDiagonal")
				{
					// move in any direction
					return getRandomMove(a);
				}
				else if (opponentCard.getName().substring(0,5) == "cMove")
				{
					if (a.getStaminaPoints() > 1) return new CardAttackCardinal();
					else return getRandomMove(a);
				}
				else if (opponentCard.getName().substring(0,5) == "cLeap")
				{
					if (a.getStaminaPoints() > 1) return new CardAttackLong();
					else return getRandomMove(a);
				}
			}
			// try to move away
			int predictedOpponentCol = o.getCol() + opponentCard.getCol();
			int predictedOpponentRow = o.getRow() + opponentCard.getRow();
			// try switching < sign
			if (Math.abs(a.getCol() - predictedOpponentCol) > Math.abs(a.getRow() - predictedOpponentRow))
			{
				// try to move horizontally
				if (a.getCol() > predictedOpponentCol)
				{
					// try to move right
					if (a.getCol() == 3) return new CardMoveRight();
					if (a.getCol() < 3) return new CardLeapRight();
				}
				else if (a.getCol() < predictedOpponentCol)
				{
					// try to move left
					if (a.getCol() == 1) return new CardMoveLeft();
					if (a.getCol() > 1) return new CardLeapLeft();
				}
			}
			else 
			{
				// try to move vertically
				if (a.getRow() > predictedOpponentRow && a.getRow() < 4) return new CardMoveUp();
				if (a.getRow() < predictedOpponentRow && a.getRow() > 0) return new CardMoveDown();
			}
			return getRandomMove(a);
		}
		// Equal
		else
		{
			System.out.println("Equal life");
			if (currentManhattan > 3) 
			{
				// if we're far away, we rest or move closer
				System.out.println("try to move closer ");
				if (o.getCol() + opponentCard.getCol() == a.getCol()) 
				{
					if (o.getRow() + opponentCard.getRow() < a.getRow()) return new CardMoveDown();
					else return new CardMoveUp();
				}
				else if (o.getRow() + opponentCard.getRow() == a.getRow()) 
				{
					if (o.getCol() + opponentCard.getCol() < a.getCol()) return new CardMoveLeft();
					else return new CardMoveRight();
				}
				// vertical move
				System.out.println("Last option");
				if (random.nextBoolean())
				{
					if (a.getRow() > o.getRow()) return new CardMoveDown();
					else return new CardMoveUp();
				}
				else
				{
					if (a.getCol() > o.getCol()) 
					{
						if (a.getCol() > 1 && random.nextBoolean()) return new CardLeapLeft();
						return new CardMoveLeft();
					}
					if (a.getCol() < 3 && random.nextBoolean()) return new CardLeapRight();
					else return new CardMoveRight();
				}
			}
			
			if (manhattan == 1 && a.getStaminaPoints() > 1) 
			{
				if (a.getCol() + opponentCard.getCol() > 0 && a.getCol() + opponentCard.getCol() < 5 && a.getRow() + opponentCard.getRow() > 0 && a.getRow() + opponentCard.getRow() < 5 && random.nextDouble() < 0.2) return opponentCard;
				return new CardAttackCardinal(); 
			}
			if ((Math.abs(a.getCol() - (o.getCol() + opponentCard.getCol())) == 1) && (Math.abs(a.getRow() - (o.getRow() + opponentCard.getRow())) == 1) && a.getStaminaPoints() > 1) 
			{
				if (a.getCol() + opponentCard.getCol() > 0 && a.getCol() + opponentCard.getCol() < 5 && a.getRow() + opponentCard.getRow() > 0 && a.getRow() + opponentCard.getRow() < 5 && random.nextDouble() < 0.2) return opponentCard;
				return new CardAttackDiagonal(); 
			}
			if (a.getRow() == o.getRow() + opponentCard.getRow() && Math.abs(a.getCol() - (o.getCol() + opponentCard.getCol())) == 2 && a.getStaminaPoints() > 1)
			{
				if(random.nextDouble() < 0.5) 
				{
					// move out of range
					double test = random.nextDouble();
					if (a.getRow() > 0 && test < 0.3) return new CardMoveDown();
					if (a.getRow() < 4 && test < 0.6) return new CardMoveUp();
					if ((a.getCol() - (o.getCol() + opponentCard.getCol())) > 0)
					{
						if (a.getCol() == 3) return new CardMoveRight();
						if (a.getCol() < 3)
						{
							if (random.nextBoolean()) return new CardMoveRight();
							else return new CardLeapRight();
						}
					}
				}
				if (a.getCol() + opponentCard.getCol() > 0 && a.getCol() + opponentCard.getCol() < 5 && a.getRow() + opponentCard.getRow() > 0 && a.getRow() + opponentCard.getRow() < 5 && random.nextDouble() < 0.2) return opponentCard;
				return new CardAttackLong();
			}
			if (manhattan != 0) 
			{
				// else move closer
				if (o.getCol() + opponentCard.getCol() == a.getCol()) 
				{
					if (o.getRow() + opponentCard.getRow() < a.getRow()) return new CardMoveDown();
					else return new CardMoveUp();
				}
				else if (o.getRow() + opponentCard.getRow() == a.getRow()) 
				{
					if (o.getCol() + opponentCard.getCol() < a.getCol()) return new CardMoveLeft();
					else return new CardMoveRight();
				}
				// vertical move
				if (random.nextBoolean())
				{
					if (a.getRow() > o.getRow()) return new CardMoveDown();
					else return new CardMoveUp();
				}
				else
				{
					if (a.getCol() > o.getCol()) 
					{
						if (a.getCol() > 1 && random.nextBoolean()) return new CardLeapLeft();
						return new CardMoveLeft();
					}
					if (a.getCol() < 3 && random.nextBoolean()) return new CardLeapRight();
					else return new CardMoveRight();
				}

			}
			if (manhattan == 0)
			{
				if (a.getStaminaPoints() > 1)
				{
					if (random.nextBoolean()) return new CardAttackLong();
					else return new CardAttackCardinal();
				}
				else 
				{
					return getRandomMove(a);
				}
			}
		}
		
		
		System.out.println("DEFAULT CARD: rest");
		return new CardRest();
	}
	
	Card getRandomMove(StateAgent a)
	{
		System.out.println("RANDOM MOVE");
		Random random = new Random();
		while(true)
		{
			double card = random.nextDouble();
			if (card < 1.0 / 6.0)
			{
				// left
				if (a.getCol() > 0) return new CardMoveLeft();
			}
			else if (card < 2.0 / 6.0)
			{
				// right
				if (a.getCol() < 4) return new CardMoveRight();
			}
			else if (card < 3.0 / 6.0)
			{
				// down
				if (a.getRow() < 4) return new CardMoveDown();
			}
			if (card < 4.0 / 6.0)
			{
				// up
				if (a.getRow() > 0) return new CardMoveUp();
			}
			if (card < 5.0 / 6.0)
			{
				// leap left
				if (a.getCol() > 1) return new CardLeapLeft();
			}
			else
			{
				// leap right
				if (a.getCol() < 3) return new CardLeapRight();
			}
		}
	}
	
	public void drawBoard(StateAgent a, StateAgent o)
	{
		for (int i = 4; i >= 0; i--)
		{
			System.out.print("|");
			for (int j = 0; j < 5; j++)
			{
				if (a.getRow() == i && a.getCol() == j) System.out.print("a");
				else if (o.getRow() == i && o.getCol() == j) System.out.print("o");
				else System.out.print(" ");
			}
			System.out.println("|");
		}
		
	}
}
