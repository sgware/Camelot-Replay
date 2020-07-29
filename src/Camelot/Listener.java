package Camelot;

import java.util.Scanner;

public class Listener extends Thread {

    public void run(){
		Scanner s = new Scanner(System.in);
		while(true) {
			String msg = s.nextLine();
			if(msg != null && CamelotReplay.replay!=null) {
				CamelotReplay.replay.Add(msg);
			}
		}
    }
  }