package itml.agents;

import itml.cards.*;
import itml.simulator.CardDeck;
import itml.simulator.StateAgent;
import itml.simulator.StateBattle;
//import itml.Predictor;
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
	private Classifier classifier2;
	//public Predictor predictor;
	Instances myInstances;
	Instances modifiedInstances;
	

	public MyAgent( CardDeck deck, int msConstruct, int msPerMove, int msLearn ) {
		super(deck, msConstruct, msPerMove, msLearn);
		J48 tree = new J48();
		tree.setMinNumObj(2);
		tree.setConfidenceFactor(0.3f);
		//MultilayerPerceptron mp = new MultilayerPerceptron();
		//mp.setTrainingTime(400);
		classifier2 = tree;
		
		//classifier2 = mp;
		//classifier2 = new IBk();
		
	}

	@Override
	public void startGame(int noThisAgent, StateBattle stateBattle) {
		// Remember the indices of the agents in the StateBattle.
		m_noThisAgent = noThisAgent;
		m_noOpponentAgent  = (noThisAgent == 0 ) ? 1 : 0; // can assume only 2 agents battling.
		//predictor.setID(m_noOpponentAgent);
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
    	if (modValues[8] < 0) modValues[8] = -1.0 * modValues[8];
    	if (modValues[9] < 0) modValues[9] = -1.0 * modValues[9];
		
		try {
			ArrayList<Card> allCards = m_deck.getCards();
			ArrayList<Card> cards = m_deck.getCards(a.getStaminaPoints());
			
			Instance currentInstance;
			currentInstance = new Instance(1.0, modValues.clone());
			currentInstance.setDataset(modifiedInstances);
			
			/*double[] probabilities;
			probabilities = classifier2.distributionForInstance(currentInstance);
			for (int i = 0; i < probabilities.length; i++)
			{
				System.out.println("Probability of card " + allCards.get(i).getName() + " : " + probabilities[i]);
			}*/
			
			int out;
			out =  (int)classifier2.classifyInstance(currentInstance);
			
			Card selected = allCards.get(out);
			if (a.getStaminaPoints() < 1) selected = new CardRest();
			//predictor.setCard(selected);
			if(cards.contains(selected)) {
				Card ourCard = getMove1(stateBattle, selected);
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

		modifiedInstances();
		modifiedInstances.setClassIndex(modifiedInstances.numAttributes() - 1);
		
		try {
			classifier2.buildClassifier(modifiedInstances);
		} catch(Exception e) {
			System.out.println("Error training classifier: " + e.toString());
		}
		
		return null;  //To change body of implemented methods use File | Settings | File Templates.
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
        	
        	if (stateRating(newState) > bestRating)
        	{
        		bestCard = card;
        		bestRating = stateRating(newState);
        	}
        }
        
        ArrayList<Card> oppCards = m_deck.getCards( asOpp.getStaminaPoints() );
        for (Card oppCard : oppCards)
        {
        	newState = (StateBattle) stateBattle.clone();
        	Card[] actions = new Card[2];
        	actions[m_noOpponentAgent] = oppCard;
        	actions[m_noThisAgent] = bestCard;
        	newState.play(actions);
        }
        Card[] actions = new Card[2];
        actions[m_noThisAgent] = bestCard;
        actions[m_noOpponentAgent] = opponentCard;
        StateBattle future = (StateBattle) stateBattle.clone();
        future.play(actions);
		
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

		// proximity
		if (asOpp.getStaminaPoints() == 0 && manhattan < 2 && asThis.getStaminaPoints() > 1) rating += 25;
		else if (asOpp.getStaminaPoints() == 0 && manhattan == 2 && asThis.getCol() != asOpp.getCol() && asThis.getStaminaPoints() > 1) rating += 25;
		else if (asOpp.getStaminaPoints() == 1 && manhattan == 0 && asThis.getStaminaPoints() > 1) rating += 15;
		
		if (asThis.getStaminaPoints() == 0 && manhattan < 2 && asOpp.getStaminaPoints() > 1) rating -= 25;
		else if (asThis.getStaminaPoints() == 0 && manhattan == 2 && asOpp.getCol() != asOpp.getCol() && asOpp.getStaminaPoints() > 1) rating -= 25;
		else if (asThis.getStaminaPoints() == 1 && manhattan == 0 && asOpp.getStaminaPoints() > 1) rating -= 15;
		
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
		if (stateBattle.getStepNumber() < stateBattle.getNumSteps() / 3) rating -= manhattan * 1;
		else if (stateBattle.getStepNumber() < 2*(stateBattle.getNumSteps() / 3) && asThis.getHealthPoints() >= asOpp.getHealthPoints()) rating -= manhattan * 2;
		else if (stateBattle.getStepNumber() < stateBattle.getNumSteps() && asThis.getHealthPoints() >= asOpp.getHealthPoints()) rating -= manhattan * 6;
		
		return rating;
	}
	
	
	
	Card getRandomMove(StateAgent a)
	{
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
		
		try {
			ArrayList<Card> allCards = m_deck.getCards();
			ArrayList<Card> cards = m_deck.getCards(a.getStaminaPoints());
			
			Instance currentInstance;
			currentInstance = new Instance(1.0, modValues.clone());
			//else currentInstance = new Instance(1.0, values.clone());
			
			currentInstance.setDataset(modifiedInstances); 
			//else currentInstance.setDataset(myInstances);
			
			int out = 0;
			out = (int) classifier2.classifyInstance(currentInstance);
			Card selected = allCards.get(out);
			if(cards.contains(selected)) {
				return selected;
			}
		} catch (Exception e) {
			System.out.println("Error2 classifying new instance: " + e.toString());
		}
		return new CardRest();  //To change body of implemented methods use File | Settings | File Templates.
	}
	/*
	public void setPredictor(Predictor p)
	{
		predictor = p;
		p.setID(m_noOpponentAgent);
	}*/

	public void printClassifier() {
		System.out.println(classifier2);
	}
	
	public void modifiedInstances()
	{
		FastVector attributes = new FastVector(); // Attributes
        FastVector actions = new FastVector();    // Class

        Attribute x = myInstances.attribute(8);
        for (int i = 0; i < x.numValues(); i++)
        {	
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
        	
        	values[11] = myInstances.attribute(8).indexOfValue(instance.stringValue(8));
        	if (instance.stringValue(8).startsWith("cAttack"))
        	{
        		// attack actions have higher weight
        		modifiedInstances.add(new Instance(1.0, values.clone()));
        	}
        	else modifiedInstances.add(new Instance(1.0, values.clone()));
        }
	}
}

