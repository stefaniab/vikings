package itml.agents;

import itml.cards.Card;
import itml.cards.CardAttackCardinal;
import itml.cards.CardAttackDiagonal;
import itml.cards.CardAttackLong;
import itml.cards.CardDefend;
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

public class AgentLazyTerminator extends Agent {

    private int m_noThisAgent;     // Index of our agent (0 or 1).
    private int m_noOpponentAgent; // Inex of opponent's agent.

    public AgentLazyTerminator( CardDeck deck, int msConstruct, int msPerMove, int msLearn ) {
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

        int dist = calcDistanceBetweenAgents(stateBattle);
        if (asThis.getStaminaPoints() > 1)
        {
        	if (asThis.getCol() == asOpp.getCol() && asThis.getRow() == asOpp.getRow())
        	{
        		return new CardAttackCardinal();
        	}
        	if (asThis.getRow() == asOpp.getRow() && dist == 2) return new CardAttackLong();
        	if (dist == 2) return new CardAttackDiagonal();
        	if (dist == 1) return new CardAttackCardinal();
        	return new CardRest();
        }
        else return new CardRest();
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
