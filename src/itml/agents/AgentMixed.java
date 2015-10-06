package itml.agents;

import itml.cards.Card;
import itml.cards.CardAttackDiagonal;
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
import java.util.Random;

public class AgentMixed extends Agent{
	
    private int m_noThisAgent;     // Index of our agent (0 or 1).
    private int m_noOpponentAgent; // Inex of opponent's agent.

    public AgentMixed( CardDeck deck, int msConstruct, int msPerMove, int msLearn ) {
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
    
    public Classifier learn( Instances instances ) {
        // No learning.
        return null;
    }
    
    public Card actChicken(StateBattle stateBattle ) {

        Card [] move = new Card[2];

        move[m_noOpponentAgent] = new CardRest();   // We assume the opponent just stays where he/she is,
                                                    // and then take the move that brings us as far away as possible.

        Card bestCard = new CardRest();
        int  minDistance = calcDistanceBetweenAgents( stateBattle );

        ArrayList<Card> cards = m_deck.getCards( stateBattle.getAgentState( m_noThisAgent ).getStaminaPoints() );
        for ( Card card : cards ) {
            StateBattle bs = (StateBattle) stateBattle.clone();   // close the state, as play( ) modifies it.
            move[m_noThisAgent] = card;
            bs.play( move );
            int  distance = calcDistanceBetweenAgents( bs );
            if ( distance > minDistance ) {
                bestCard = card;
                minDistance = distance;
            }
        }

        return bestCard;
    }
    
    public Card actTerminator(StateBattle stateBattle) {

        StateAgent asThis = stateBattle.getAgentState( m_noThisAgent );
        StateAgent asOpp  = stateBattle.getAgentState( m_noOpponentAgent );

        ArrayList<Card> cards = m_deck.getCards( asThis.getStaminaPoints() );

        // First check to see if we are in attack range, if so attack.
        for ( Card card : cards ) {
            if ( (card.getType() == Card.CardActionType.ctAttack) &&
                  card.inAttackRange( asThis.getCol(), asThis.getRow(),
                                      asOpp.getCol(), asOpp.getRow() ) ) {
                return card;  // attack!
            }
        }

        // If we cannot attack, then try to move closer to the agent.
        Card [] move = new Card[2];
        move[m_noOpponentAgent] = new CardRest();  

        Card bestCard = new CardRest();
        int  bestDistance = calcDistanceBetweenAgents( stateBattle );

        // ... otherwise move closer to the opponent.
        for ( Card card : cards ) {
            StateBattle bs = (StateBattle) stateBattle.clone();   // close the state, as play( ) modifies it.
            move[m_noThisAgent] = card;
            bs.play( move );
            int  distance = calcDistanceBetweenAgents( bs );
            if ( distance < bestDistance ) {
                bestCard = card;
                bestDistance = distance;
            }
        }

        return bestCard;
    }
    
    public Card act(StateBattle stateBattle)
    {
    	StateAgent a = stateBattle.getAgentState(m_noThisAgent);
    	StateAgent o = stateBattle.getAgentState(m_noOpponentAgent);
    	if(a.getStaminaPoints() <= 0) return new CardRest();
    	if (calcDistanceBetweenAgents(stateBattle) > 2) return actChicken(stateBattle);
    	else return actTerminator(stateBattle);
    }
    
    private int calcDistanceBetweenAgents( StateBattle bs ) {

        StateAgent asFirst = bs.getAgentState( 0 );
        StateAgent asSecond = bs.getAgentState( 1 );

        return Math.abs( asFirst.getCol() - asSecond.getCol() ) + Math.abs( asFirst.getRow() - asSecond.getRow() );
    }
}
