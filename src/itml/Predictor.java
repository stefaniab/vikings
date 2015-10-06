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
	
	int[][] misclassification;
	int[] correctClass;
	boolean waitingForGuess = true;
	
	public Predictor(MyAgent agent)
	{
		this.right = 0;
		this.wrong = 0;
		//this.id = id;
		guess = new ArrayList<Card>();
		actualCard = new ArrayList<Card>();
		this.agent = agent;
		
		// attack, defend, move, rest
		misclassification = new int[4][4];
		correctClass = new int[4];
		for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) misclassification[i][j] = 0;
		for (int i = 0; i < 4; i++) correctClass[i] = 0;
	}
	public void setCard(Card card)
	{
		System.out.println("setCard");
		if (!waitingForGuess) 
			System.out.print("ERROR, out of order");
		lastCard = card;
		waitingForGuess = false;
	}
	public void checkGuess(Card card, int id)
	{
		// attack, defend, move, rest
		if (this.id != id) return;
		System.out.println("checkGuess");
		if (waitingForGuess) 
			System.out.print("ERROR, out of order");
		waitingForGuess = true;
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
			if (card.getName().startsWith("cAttack")) correctClass[0]++;
			else if (card.getName().startsWith("cDefend")) correctClass[1]++;
			else if (card.getName().startsWith("cMove") || card.getName().startsWith("cLeap")) correctClass[2]++;
			else correctClass[3]++;
		}
		else 
		{
			wrong++;
			System.out.println("Our guess " + lastCard.getName() + " real card " + card.getName());
			System.out.println("INCORRECT GUESS");
			int actual = 0;
			if (card.getName().startsWith("cAttack")) actual = 0;
			else if (card.getName().startsWith("cDefend")) actual = 1;
			else if (card.getName().startsWith("cMove") || card.getName().startsWith("cLeap")) actual = 2;
			else actual = 3;
			int prediction = 0;
			if (lastCard.getName().startsWith("cAttack")) prediction = 0;
			else if (lastCard.getName().startsWith("cDefend")) prediction = 1;
			else if (lastCard.getName().startsWith("cMove") || lastCard.getName().startsWith("cLeap")) prediction = 2;
			else prediction = 3;
			misclassification[actual][prediction]++;
		}
		lastCard = null;
	}
	public void print()
	{
		//agent.printClassifier();
		System.out.println();
		System.out.println("ACCURCY");
		System.out.println("right : " + right + " \t wrong " + wrong + " \t ratio : " + right * 1.0 / (right + wrong));
		System.out.println("CORRECTLY CLASSIFIED:");
		System.out.println("ATTACK " + correctClass[0]);
		System.out.println("DEFEND " + correctClass[1]);
		System.out.println("MOVE/LEAP " + correctClass[2]);
		System.out.println("REST " + correctClass[3]);
		System.out.println("INCORRECTLY CLASSIFIED:");
		System.out.println("actual class-> \t attack \t defend \t move/leap \t rest");
		System.out.print("attack: ");
		for (int i = 0; i < 4; i++) System.out.print("\t\t" + misclassification[i][0]);
		System.out.println();
		System.out.print("defend: ");
		for (int i = 0; i < 4; i++) System.out.print("\t\t" + misclassification[i][1]);
		System.out.println();
		System.out.print("move/leap: ");
		for (int i = 0; i < 4; i++) System.out.print("\t\t" + misclassification[i][2]);
		System.out.println();
		System.out.print("rest: \t");
		for (int i = 0; i < 4; i++) System.out.print("\t\t" + misclassification[i][3]);
	}
	
	public void setID(int id)
	{
		this.id = id;
		waitingForGuess = true;
	}
	
	
	
}
