package itml;
import java.util.ArrayList;

import itml.agents.MyAgent;
import itml.cards.*;

public class Predictor {
	int right;
	int wrong;
	Card lastCard;
	int id;
	ArrayList<Card> guess;
	ArrayList<Card> actualCard;
	MyAgent agent;
	
	public Predictor(MyAgent agent)
	{
		this.right = 0;
		this.wrong = 0;
		//this.id = id;
		guess = new ArrayList<Card>();
		actualCard = new ArrayList<Card>();
		this.agent = agent;
	}
	public void setCard(Card card)
	{
		lastCard = card;
	}
	public void checkGuess(Card card, int id)
	{
		if (this.id != id) return;
		if (card == null) 
		{
			System.out.println("NULL GUESS");
			return;
		}
		if (lastCard == null)
		{
			System.out.println("NULL LAST GUESS");
			return;
		}
		if (lastCard.getName() == card.getName())
		{
			System.out.println("CORRECT GUESS");
			right++;
		}
		else 
		{
			wrong++;
			System.out.println("INCORRECT GUESS");
		}
		lastCard = null;
	}
	public void print()
	{
		//agent.printClassifier();
		System.out.println();
		System.out.println("ACCURCY");
		System.out.println("right : " + right + " \t wrong " + wrong + " \t ratio : " + right * 1.0 / (right + wrong));
	}
	
	public void setID(int id)
	{
		this.id = id;
	}
	
	
}