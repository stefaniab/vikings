package itml.agents;

import java.util.ArrayList;
import java.util.Random;

import itml.cards.Card;
import itml.cards.CardAttackCardinal;
import itml.cards.CardAttackDiagonal;
import itml.cards.CardAttackLong;
import itml.cards.CardDefend;
import itml.cards.CardLeapLeft;
import itml.cards.CardLeapRight;
import itml.cards.CardMoveDown;
import itml.cards.CardMoveLeft;
import itml.cards.CardMoveRight;
import itml.cards.CardMoveUp;
import itml.cards.CardRest;
import itml.simulator.CardDeck;
import itml.simulator.StateAgent;
import itml.simulator.StateBattle;
import weka.classifiers.Classifier;
import weka.core.Instances;

/**
 *
 *  Example agent:
 *
 * This agent is simply a sitting duck.
 *
 * @author      Yngvi Bjornsson
 *
 * @version     %I%, %G%
 *
 */
public class AgentTest1 extends Agent {
	private int m_noThisAgent;     // Index of our agent (0 or 1).
	private int m_noOpponentAgent; // Inex of opponent's agent.

    public AgentTest1( CardDeck deck, int msConstruct, int msPerMove, int msLearn ) {
        super(deck, msConstruct, msPerMove, msLearn );
    }

    public void startGame(int noThisAgent, StateBattle stateBattle) {
        // No book-keeping needed.
    }

    public void endGame( StateBattle stateBattle, double [] results ) {
        // No book-keeping needed.
    }

    public Card act( StateBattle stateBattle ) {
    	StateAgent asThis = stateBattle.getAgentState( m_noThisAgent );
        StateAgent asOpp  = stateBattle.getAgentState( m_noOpponentAgent );

        ArrayList<Card> cards = m_deck.getCards( asThis.getStaminaPoints() );

        // for each of our moves, what is the worst the opponent could do
        int bestRating = -100000;
        Card bestCard = null;
        for (int i = 0; i < cards.size(); i++)
        {
        	Card ourCard = cards.get(i);
        	int cardRating = 1000000;
        	for (int j = 0; j < cards.size(); j++)
        	{
        		StateBattle newState = (StateBattle) stateBattle.clone();
        		Card[] playedCards = new Card[2];
        		playedCards[m_noThisAgent] = ourCard;
        		playedCards[m_noOpponentAgent] = cards.get(j);
        		newState.play(playedCards);
        		int rating = stateRating(newState);
        		if (rating < cardRating) cardRating = rating;
        	}
        	if (cardRating > bestRating)
        	{
        		bestRating = cardRating;
        		bestCard = ourCard;
        	}
        }
        return bestCard;
    }

    public Classifier learn( Instances instances  ) {
        // Too lazy to learn anything.
        return null;
    }
    
    private Card getMove(StateBattle stateBattle, Card opponentCard)
	{
		
		Random random = new Random();
		StateAgent a = stateBattle.getAgentState(m_noThisAgent);
		StateAgent o = stateBattle.getAgentState(m_noOpponentAgent);

		//drawBoard(a, o);
		
		int manhattan = Math.abs(o.getCol() + opponentCard.getCol() - a.getCol()) + Math.abs(o.getRow() + opponentCard.getRow() - a.getRow());
		int currentManhattan = Math.abs(o.getCol() - a.getCol()) + Math.abs(o.getRow() - a.getRow());
		
		//System.out.print("BATTLESTATE: health/stamina a: " + a.getHealthPoints() + " " + a.getStaminaPoints() + " o: "+ o.getHealthPoints() + " " + o.getStaminaPoints() + " ");
		//System.out.print("Location: " + a.getRow() + " " + a.getCol() + " " + o.getRow() + " " + o.getCol() + " ");
		
		//System.out.println("current manhattan : " + currentManhattan + " predicted manhattan : " + manhattan);
		
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
			//System.out.println("AGGRESSIVE");
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
			//System.out.println("DEFENSIVE");
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
			//System.out.println("Equal life");
			if (currentManhattan > 3) 
			{
				// if we're far away, we rest or move closer
				//System.out.println("try to move closer ");
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
				//System.out.println("Last option");
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
		
		
		//System.out.println("DEFAULT CARD: rest");
		return new CardRest();
	}
    
    Card getRandomMove(StateAgent a)
	{
		//System.out.println("RANDOM MOVE");
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
    
    public int stateRating(StateBattle stateBattle)
	{
		StateAgent asThis = stateBattle.getAgentState( m_noThisAgent );
        StateAgent asOpp  = stateBattle.getAgentState( m_noOpponentAgent );
        if (asThis.getHealthPoints() == 0 && asOpp.getHealthPoints() > 0 ) return -1000;
        else if (asOpp.getHealthPoints() == 0 && asThis.getHealthPoints() > 0 ) return 1000;
		
        int rating = 0;
		Random random = new Random();
		rating += random.nextInt(10);
		// health difference
		rating += 50 * (asThis.getHealthPoints() - asOpp.getHealthPoints());
		// stamina difference
		//rating += 5 * (Math.min(asThis.getStaminaPoints(), 10) - Math.min(10, asOpp.getStaminaPoints()));
		rating += 8 * Math.floor(Math.sqrt(asThis.getStaminaPoints()) - Math.sqrt(asOpp.getStaminaPoints()));
		// proximity
		int manhattan = Math.abs(asThis.getCol() - asOpp.getCol()) + Math.abs(asThis.getRow() - asOpp.getRow());
		
		// Testing stamina penalty
		if (asThis.getStaminaPoints() < 2) rating -= 30;
		
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
		else if (asThis.getHealthPoints() != asOpp.getHealthPoints() && asThis.getStaminaPoints() != asOpp.getStaminaPoints())
		{
			rating += manhattan * 5;
		}
		else rating -= manhattan * 2;
		//System.out.println(stateBattle);
		//System.out.println("has rating " + rating);
		
		return rating;
	}
	
	

}
