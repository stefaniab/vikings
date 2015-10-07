package itml.agents;

import itml.cards.*;
import itml.simulator.CardDeck;
import itml.simulator.StateAgent;
import itml.simulator.StateBattle;
import itml.Predictor;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.lazy.IB1;
import weka.classifiers.lazy.IBk;
import weka.classifiers.pmml.consumer.NeuralNetwork;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.ADTree;
import weka.classifiers.trees.BFTree;
import weka.classifiers.trees.DecisionStump;
import weka.classifiers.trees.FT;
import weka.classifiers.trees.Id3;
import weka.classifiers.trees.J48graft;
import weka.classifiers.trees.LADTree;
import weka.classifiers.trees.LMT;
import weka.classifiers.trees.NBTree;
import weka.classifiers.trees.RandomForest;
import weka.classifiers.trees.RandomTree;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.Random;

/**
 * User: deong
 * Date: 9/28/14
 */
public class MyAgent extends Agent {
	private int m_noThisAgent;     // Index of our agent (0 or 1).
	private int m_noOpponentAgent; // Inex of opponent's agent.
	private Classifier classifier_;
	private Classifier classifier2;
	public Predictor predictor;
	Instances myInstances;
	Instances modifiedInstances;
	
	boolean useModified = true;

	public MyAgent( CardDeck deck, int msConstruct, int msPerMove, int msLearn ) {
		super(deck, msConstruct, msPerMove, msLearn);
		J48 tree = new J48();
		tree.setMinNumObj(2);
		tree.setConfidenceFactor(0.3f);
		//classifier_ = tree;
		//J48 tree2 = new J48();
		//tree2.setMinNumObj(30);
		//tree2.setConfidenceFactor(0.3f);
		//classifier2 = tree2;
		//MultilayerPerceptron mp = new MultilayerPerceptron();
		//mp.setTrainingTime(400);
		//classifier2 = tree;
		classifier2 = tree;
		
		//classifier2 = mp;
		//classifier_ = new NaiveBayes();
		//classifier2 = new IBk();
		// J48
		
		//if (useModified) classifier_ = classifier2;
		
	}

	@Override
	public void startGame(int noThisAgent, StateBattle stateBattle) {
		// Remember the indicies of the agents in the StateBattle.
		m_noThisAgent = noThisAgent;
		m_noOpponentAgent  = (noThisAgent == 0 ) ? 1 : 0; // can assume only 2 agents battling.
		predictor.setID(m_noOpponentAgent);
	}

	@Override
	public void endGame(StateBattle stateBattle, double[] results) {
		//To change body of implemented methods use File | Settings | File Templates.
		// predictor.print();
	}

	@Override
	public Card act(StateBattle stateBattle) {
		System.out.println(stateBattle);
		StateAgent a = stateBattle.getAgentState(m_noOpponentAgent);
		StateAgent o = stateBattle.getAgentState(m_noThisAgent);
		
		double[] values = new double[8];
		values[0] = a.getCol();
		values[1] = a.getRow();
		values[2] = a.getHealthPoints();
		values[3] = a.getStaminaPoints();
		values[4] = o.getCol();
		values[5] = o.getRow();
		values[6] = o.getHealthPoints();
		values[7] = o.getStaminaPoints();
		
		double[] modValues = new double[11];
		if (useModified)
		{
			modValues[0] = a.getCol();
			modValues[1] = a.getRow();
			modValues[2] = a.getHealthPoints();
			modValues[3] = a.getStaminaPoints();
			modValues[4] = o.getCol();
			modValues[5] = o.getRow();
			modValues[6] = o.getHealthPoints();
			modValues[7] = o.getStaminaPoints();
			modValues[8] = modValues[0] - modValues[4];
			modValues[9] = modValues[1] - modValues[5];
			modValues[10] = modValues[2] - modValues[6];
			/*
			if (modValues[1] == 0) {}
        	else if (modValues[4] > 3.5) modValues[4] = 2.0;
        	else modValues[4] = 1.0;
        	if (modValues[5] == 0) {}
        	else if (modValues[5] > 3.5) modValues[5] = 2.0;
        	else modValues[5] = 1.0;
			*/
        	if (modValues[8] < 0) modValues[8] = -1.0 * modValues[8];
        	if (modValues[9] < 0) modValues[9] = -1.0 * modValues[9];
		}
		try {
			ArrayList<Card> allCards = m_deck.getCards();
			ArrayList<Card> cards = m_deck.getCards(a.getStaminaPoints());
			
			Instance currentInstance;
			if (!useModified) currentInstance = new Instance(1.0, values.clone());
			else currentInstance = new Instance(1.0, modValues.clone());
			if (!useModified) currentInstance.setDataset(myInstances);
			else currentInstance.setDataset(modifiedInstances);
			
			/*double[] probabilities;
			if (!useModified) probabilities = classifier_.distributionForInstance(currentInstance);
			else probabilities = classifier2.distributionForInstance(currentInstance);
			for (int i = 0; i < probabilities.length; i++)
			{
				System.out.println("Probability of card " + allCards.get(i).getName() + " : " + probabilities[i]);
			}*/
			
			int out;
			if (!useModified) out = (int)classifier_.classifyInstance(currentInstance);
			else out =  (int)classifier2.classifyInstance(currentInstance);
			
			Card selected = allCards.get(out);
			if (a.getStaminaPoints() < 1) selected = new CardRest();
			predictor.setCard(selected);
			System.out.print("Predicted opponent card: " + selected.getName() + " ");
			if(cards.contains(selected)) {
				//return selected;
				Card ourCard = getMove1(stateBattle, selected);
				System.out.println("Our card: " + ourCard.getName());
				//System.out.println("Predicted opponent card: " + selected.getName());
				//return selected;
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
		
		/*System.out.println("ORIGINAL");

		for (int i = 0; i < myInstances.numInstances(); i++)
		{
			Instance instance = myInstances.instance(i);
			for (int j = 0; j < instance.numAttributes(); j++)
			{
				System.out.print(instance.value(j) + " ");
			}
			System.out.println();
		}*/
		
		if (useModified) 
		{
			modifiedInstances();
			modifiedInstances.setClassIndex(modifiedInstances.numAttributes() - 1);

			/*System.out.println("MODIFIED");
>>>>>>> c05a371cacbb80dc56ebfbd4bc9aa902dc81f698
			for (int i = 0; i < modifiedInstances.numInstances(); i++)
			{
				Instance instance = modifiedInstances.instance(i);
				for (int j = 0; j < instance.numAttributes(); j++)
				{
					System.out.print(instance.value(j) + " ");
				}
				System.out.println();
<<<<<<< HEAD
			}
=======
			}*/
		}
		
		try {
			//classifier_.buildClassifier(instances);
			if (useModified) classifier2.buildClassifier(modifiedInstances);
		} catch(Exception e) {
			System.out.println("Error training classifier: " + e.toString());
		}
		//System.out.println(classifier_);
		if (useModified) System.out.println(classifier2);
		
		// if (useModified) classifier_ = classifier2;
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}
	
	private Card getMove(StateBattle stateBattle, Card opponentCard)
	{
		
		Random random = new Random();
		StateAgent a = stateBattle.getAgentState(m_noThisAgent);
		StateAgent o = stateBattle.getAgentState(m_noOpponentAgent);

		//drawBoard(a, o);
		
		int manhattan = Math.abs(o.getCol() + opponentCard.getCol() - a.getCol()) + Math.abs(o.getRow() + opponentCard.getRow() - a.getRow());
		int currentManhattan = Math.abs(o.getCol() - a.getCol()) + Math.abs(o.getRow() - a.getRow());
		
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
	
	private Card getMove1(StateBattle stateBattle, Card opponentCard)
	{
		StateAgent asThis = stateBattle.getAgentState( m_noThisAgent );
		StateAgent asOpp = stateBattle.getAgentState(m_noOpponentAgent);
        ArrayList<Card> cards = m_deck.getCards( asThis.getStaminaPoints() );
		// assume opponentCard is chosen
		// for each possible card, compute state
        StateBattle newState = null;
        Card bestCard = null;
        int bestRating = -100000;
        for (Card card : cards)
        {
        	newState = (StateBattle) stateBattle.clone();
        	Card[] chosenCards = new Card[2];
        	chosenCards[m_noThisAgent] = card;
        	chosenCards[m_noOpponentAgent] = opponentCard;
        	newState.play(chosenCards);
        	/*
        	// 2 STEP SEARCH
        	// get opponent card
        	Card newOpponentCard = getOpponentCard(newState);
        	System.out.println("Next level opponent move " + newOpponentCard.getName());
        	StateAgent newAsThis = newState.getAgentState(m_noThisAgent);

        	// get set of possible cards
        	ArrayList<Card> newCards = m_deck.getCards(newAsThis.getStaminaPoints());
        	// loop through them, generate state, rate it
        	//Card newBestCard = null;
        	int newBestRating = -100000;
        	StateBattle newNewState = null;
        	for (Card newCard : newCards)
        	{
        		newNewState = (StateBattle) newState.clone();

        		Card[] newChosenCards = new Card[2];
        		newChosenCards[m_noThisAgent] = newCard;
        		newChosenCards[m_noOpponentAgent] = newOpponentCard;
        		newNewState.play(newChosenCards);
        		int newStateRating = stateRating(newNewState);
        		if (newStateRating > newBestRating)
        		{
        			//newBestCard = newCard;
        			newBestRating = newStateRating;
        		}
        		System.out.println("\t LEVEL 2 OF SEARCH " + newCard.getName() + " rating " + newStateRating);
        	}*/
        	//System.out.println("LEVEL 1 OF SEARCH " + card.getName() + " rating " + newBestRating);
        	//if (newBestRating > bestRating)
        	
        	if (stateRating(newState) > bestRating)
        	{
        		bestCard = card;
        		//bestRating = newBestRating;
        		bestRating = stateRating(newState);
        		//bestRating = newBestRating;
        	}
        }
        //System.out.println("best card is " + bestCard.getName() + " with rating " + bestRating);
        
        ArrayList<Card> oppCards = m_deck.getCards( asOpp.getStaminaPoints() );
        for (Card oppCard : oppCards)
        {
        	newState = (StateBattle) stateBattle.clone();
        	Card[] actions = new Card[2];
        	actions[m_noOpponentAgent] = oppCard;
        	actions[m_noThisAgent] = bestCard;
        	newState.play(actions);
        	System.out.print("o act " + oppCard.getName() + " r " + stateRating(newState) + " ");
        	if (stateRating(newState) < -1000) 
        	{
        		System.out.println("LOSING MOVE");
        		System.out.println("LOSING MOVE");
        	}
        }
        Card[] actions = new Card[2];
        actions[m_noThisAgent] = bestCard;
        actions[m_noOpponentAgent] = opponentCard;
        StateBattle future = (StateBattle) stateBattle.clone();
        future.play(actions);
        System.out.println();
        System.out.println("Predicted next move " + getOpponentCard(future).getName());
		
        return bestCard;
	}
	
	public int stateRating(StateBattle stateBattle)
	{
		StateAgent asThis = stateBattle.getAgentState( m_noThisAgent );
        StateAgent asOpp  = stateBattle.getAgentState( m_noOpponentAgent );
        
        //are we either loosing or winning
        if (asThis.getHealthPoints() == 0 && asOpp.getHealthPoints() > 0 ) return -1000;
        else if (asOpp.getHealthPoints() == 0 && asThis.getHealthPoints() > 0 ) return 1000;
        int manhattan = Math.abs(asThis.getCol() - asOpp.getCol()) + Math.abs(asThis.getRow() - asOpp.getRow());
        int rating = 0;
		Random random = new Random();
		rating += random.nextInt(10);
		
		// Health difference
		rating += 50 * (asThis.getHealthPoints() - asOpp.getHealthPoints());
		
		// Stamina difference
		rating += 5 * (Math.min(asThis.getStaminaPoints(), 10) - Math.min(10, asOpp.getStaminaPoints()));
		
		if (asOpp.getStaminaPoints() == 0 && manhattan < 2 && asThis.getStaminaPoints() > 1) rating += 25;
		else if (asOpp.getStaminaPoints() == 0 && manhattan == 2 && asThis.getCol() != asOpp.getCol() && asThis.getStaminaPoints() > 1) rating += 25;
		else if (asOpp.getStaminaPoints() == 1 && manhattan == 0 && asThis.getStaminaPoints() > 1) rating += 15;
		
		
		// Testing stamina penalty
		if (asThis.getStaminaPoints() < 2) rating -= 20;
		
		// Try to stay close to the middle if we have the upper hand
		if (asThis.getHealthPoints() > asOpp.getHealthPoints() ||
				asThis.getHealthPoints() == asOpp.getHealthPoints() && asThis.getStaminaPoints() > asOpp.getStaminaPoints() )
		{
			int x = Math.abs(2 - asThis.getCol());
			int y = Math.abs(2 - asThis.getRow());
			if (x == 0) rating += 3;
			else if (x == 1) rating += 2;
			if (y == 0) rating += 3;
			else if (y == 1) rating += 2;
			rating -= manhattan * 5;
		}
		// Try to flee if the opponent has the upper hand
		else if (asThis.getHealthPoints() != asOpp.getHealthPoints() && asThis.getStaminaPoints() != asOpp.getStaminaPoints())
		{
			rating += manhattan * 5;
		}
		//Get more aggressive after more steps
		if (stateBattle.getStepNumber() < 10) rating -= manhattan * 1;
		else if (stateBattle.getStepNumber() < 20 && asThis.getHealthPoints() >= asOpp.getHealthPoints()) rating -= manhattan * 2;
		else if (stateBattle.getStepNumber() < 30 && asThis.getHealthPoints() >= asOpp.getHealthPoints()) rating -= manhattan * 6;
		//System.out.println(stateBattle);
		//System.out.println("has rating " + rating);
		
		return rating;
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
	

	
	public Card getOpponentCard(StateBattle stateBattle)
	{
		StateAgent asOpp = stateBattle.getAgentState(m_noOpponentAgent);
		if (asOpp.getStaminaPoints() < 1) return new CardRest();
		
		double[] values = new double[8];
		StateAgent a = stateBattle.getAgentState(m_noOpponentAgent);
		StateAgent o = stateBattle.getAgentState(m_noThisAgent);
		values[0] = a.getCol();
		values[1] = a.getRow();
		values[2] = a.getHealthPoints();
		values[3] = a.getStaminaPoints();
		values[4] = o.getCol();
		values[5] = o.getRow();
		values[6] = o.getHealthPoints();
		values[7] = o.getStaminaPoints();
		double[] modValues = new double[11];
		if (useModified)
		{
			modValues[0] = a.getCol();
			modValues[1] = a.getRow();
			modValues[2] = a.getHealthPoints();
			modValues[3] = a.getStaminaPoints();
			modValues[4] = o.getCol();
			modValues[5] = o.getRow();
			modValues[6] = o.getHealthPoints();
			modValues[7] = o.getStaminaPoints();
			modValues[8] = modValues[0] - modValues[4];
			modValues[9] = modValues[1] - modValues[5];
			modValues[10] = modValues[2] - modValues[6];
		}
		try {
			ArrayList<Card> allCards = m_deck.getCards();
			ArrayList<Card> cards = m_deck.getCards(a.getStaminaPoints());
			
			Instance currentInstance;
			if (useModified) currentInstance = new Instance(1.0, modValues.clone());
			else currentInstance = new Instance(1.0, values.clone());
			
			if (useModified) currentInstance.setDataset(modifiedInstances); 
			else currentInstance.setDataset(myInstances);
			
			//for (int i = 0; i < 8; i++) System.out.println(values[i] + " ");
			int out = 0;
			if (useModified) out = (int) classifier2.classifyInstance(currentInstance);
			//else out = (int)classifier_.classifyInstance(currentInstance);
			Card selected = allCards.get(out);
			if(cards.contains(selected)) {
				return selected;
			}
		} catch (Exception e) {
			System.out.println("Error2 classifying new instance: " + e.toString());
		}
		return new CardRest();  //To change body of implemented methods use File | Settings | File Templates.
	}
	
	public void setPredictor(Predictor p)
	{
		predictor = p;
		p.setID(m_noOpponentAgent);
	}

	public void printClassifier() {
		if (useModified) System.out.println(classifier2);
		//else System.out.println(classifier_);
	}
	
	public void modifiedInstances()
	{
		FastVector attributes = new FastVector(); // Attributes
        FastVector actions = new FastVector();    // Class

        // Domain of class are all possible cards.
        /*for ( Card c : deck.getCards() ) {
            actions.addElement( c.getName() );
        }*/
        Attribute x = myInstances.attribute(8);
        for (int i = 0; i < x.numValues(); i++)
        {	
        	//System.out.println(x.value(i));
        	actions.addElement(x.value(i));
        }

        // Add the predicting attributes (all numeric)
        attributes.addElement(new Attribute("a_x"));
        attributes.addElement(new Attribute("a_y"));
        attributes.addElement(new Attribute("a_health"));
        attributes.addElement(new Attribute("a_stamina"));
        attributes.addElement(new Attribute("o_x"));
        attributes.addElement(new Attribute("o_y"));
        attributes.addElement(new Attribute("o_health"));
        attributes.addElement(new Attribute("o_stamina"));
        attributes.addElement(new Attribute("delta_x"));
        attributes.addElement(new Attribute("delta_y"));
        attributes.addElement(new Attribute("delta_h"));
        // Add the class, the action the a_ agent took in the given state (nominal).
        attributes.addElement(new Attribute("a_action", actions));
        
        modifiedInstances = new Instances( "ModAgentBattleHistory", attributes, 0 );
        
        double[] values = new double[myInstances.numAttributes() + 3];
        for (int i = 0; i < myInstances.numInstances(); i++)
        {
        	Instance instance = myInstances.instance(i);
        	for (int j = 0; j < 8; j++)
        	{
        		values[j] = instance.value(j);
        	}
        	
        	
        	values[8] = values[0] - values[4];
        	values[9] = values[1] - values[5];
        	if (values[8] < 0) values[8] = -1.0 * values[8];
        	if (values[9] < 0) values[9] = -1.0 * values[9];
        	values[10] = values[2] - values[6];
        	/*
        	// experiment - simplify the rows
        	if (values[1] == 0) {}
        	else if (values[4] > 3.5) values[4] = 2.0;
        	else values[4] = 1.0;
        	if (values[5] == 0) {}
        	else if (values[5] > 3.5) values[5] = 2.0;
        	else values[5] = 1.0;*/
        	
        	values[11] = myInstances.attribute(8).indexOfValue(instance.stringValue(8));
        	if (instance.stringValue(8).startsWith("cAttack"))
        	{
        		//System.out.println(instance.stringValue(8));
        		// attack actions have higher weight
        		modifiedInstances.add(new Instance(1.0, values.clone()));
        	}
        	else modifiedInstances.add(new Instance(1.0, values.clone()));
        }
	}
}

