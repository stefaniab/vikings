package itml.agents;

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

import java.util.ArrayList;

/**
 *
 *  Example agent:
 *
 * This agent is very aggressive, runs directly towards the opponent and
 * once the opponent is in range attacks.
 *
 * @author      Yngvi Bjornsson
 *
 * @version     %I%, %G%
 *
 */

public class AgentTest2 extends Agent {

    private int m_noThisAgent;     // Index of our agent (0 or 1).
    private int m_noOpponentAgent; // Inex of opponent's agent.

    public AgentTest2( CardDeck deck, int msConstruct, int msPerMove, int msLearn ) {
        super(deck, msConstruct, msPerMove, msLearn );
    }

    public void startGame(int noThisAgent, StateBattle stateBattle) {
        // Remember the indicies of the agents in the StateBattle.
        m_noThisAgent = noThisAgent;
        m_noOpponentAgent  = (noThisAgent == 0 ) ? 1 : 0; // can assume only 2 agents battling.
    }

    public void endGame(StateBattle stateBattle, double[] results) {
        // Nothing to do.
    }

    public Card act(StateBattle stateBattle) {

        StateAgent asThis = stateBattle.getAgentState( m_noThisAgent );
        StateAgent asOpp  = stateBattle.getAgentState( m_noOpponentAgent );
        
        int d_col = asThis.getCol() - asOpp.getCol();
        int d_row = asThis.getRow() - asOpp.getRow();
        int d_health = asThis.getHealthPoints() - asOpp.getHealthPoints();
        int dist = calcDistanceBetweenAgents(stateBattle);
        
        if (asThis.getStaminaPoints() < 1) return new CardRest();
        if (d_col == 0 && d_row == 0) 
        {
        	if (d_health > 0 && asThis.getStaminaPoints() > 1) return new CardAttackCardinal();
        	else if (d_health == 0 && asThis.getStaminaPoints() > asOpp.getStaminaPoints()) return new CardAttackCardinal();
        	else if (asThis.getStaminaPoints() > 1 && asOpp.getStaminaPoints() > 1) return new CardDefend();
        	else if (asThis.getCol() > 1) return new CardLeapLeft();
        	else return new CardLeapRight();
        }
        if (dist == 2 && d_row != 0)
        {
        	if (asOpp.getStaminaPoints() < 2) 
        	{
        		if (asThis.getCol() > 1) return new CardLeapLeft();
        		else return new CardLeapRight();
        	}
        	if (asThis.getStaminaPoints() > 1) return new CardAttackDiagonal();
        	if (d_col > 0 && asThis.getCol() < 3) return new CardLeapRight();
        	if (d_col < 0 && asThis.getCol() > 1) return new CardLeapLeft();
        	else if (d_row > 0 && asThis.getRow() > 0) return new CardMoveUp();
        	else if (d_row < 0 && asThis.getRow() < 4) return new CardMoveUp();
        	
        }
        if (d_row == 0 && dist > 1)
        {
        	if (asThis.getRow() == 0 || asThis.getRow() == 3) return new CardMoveDown();
        	else return new CardMoveUp();
        }
        if (dist == 1)
        {
        	if (asThis.getStaminaPoints() == 1 || d_health < 0)
        	{
        		// move away
        		if (d_row == 1)
        		{
        			if (asThis.getRow() > 0) return new CardMoveUp();
        			else if (asThis.getCol() == 1) return new CardMoveLeft();
        			else if (asThis.getCol() == 0 || asThis.getCol() == 2) return new CardLeapRight();
        			return new CardLeapLeft();
        		}
        	}
        	return new CardAttackCardinal();
        }
        if (dist > 2) return new CardRest();
        
        return new CardRest();
    }

    public Classifier learn( Instances instances ) {
        // No learning.
        return null;
    }

    private int calcDistanceBetweenAgents( StateBattle bs ) {

        StateAgent asFirst = bs.getAgentState( 0 );
        StateAgent asSecond = bs.getAgentState( 1 );

        return Math.abs( asFirst.getCol() - asSecond.getCol() ) + Math.abs( asFirst.getRow() - asSecond.getRow() );
    }

}
