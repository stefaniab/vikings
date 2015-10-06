package itml.agents;

import itml.cards.Card;
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

public class AgentCopycat extends Agent {

    private int m_noThisAgent;     // Index of our agent (0 or 1).
    private int m_noOpponentAgent; // Inex of opponent's agent.

    public AgentCopycat( CardDeck deck, int msConstruct, int msPerMove, int msLearn ) {
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

        ArrayList<Card> cards = m_deck.getCards( asThis.getStaminaPoints() );

        Card lastOpp = stateBattle.getLastMoves()[m_noOpponentAgent];
        if (lastOpp == null) return new CardRest();
        if (lastOpp.getStaminaPoints() > asThis.getStaminaPoints()) return new CardRest();
        int col = asThis.getCol() + lastOpp.getCol();
        int row = asThis.getRow() + lastOpp.getRow();
        if (col < 0 || col > 4 || row < 0 || row > 4) return new CardRest();
        return lastOpp;
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
