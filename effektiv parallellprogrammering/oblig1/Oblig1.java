import java.util.*;
import java.util.concurrent.*;

class Oblig1{
    CyclicBarrier b;
    static int numIter = 9; // gitt i oppgaveteksten
    int antKjerner,size;
    int antTraader;
    static int typeSortering;
    int[] arraySize,s1,s2,s3;
    long[] innebygd,sekvensiell,parallell;
    long[] innebygdM,sekvensiellM,parallellM;
    Oblig1(){
	arraySize = new int[] {1000,10000,100000,1000000,10000000,100000000};
	innebygd = new long[numIter];
	sekvensiell = new long[numIter];
	parallell = new long[numIter]; 
	innebygdM = new long[numIter];
	sekvensiellM = new long[numIter];
	parallellM = new long[numIter];
	antKjerner= Runtime.getRuntime().availableProcessors();
    }
    public static void main(String[] args){
	if (args.length!=1){
	    System.out.println("Use command java Oblig1 <type sortering>");
	    System.out.println("tilgjengelig er: "+"\n"+"1 Java Arraysort");
	    System.out.println("2 Sekvensiell"+"\n"+"3 Parallell");
	    System.out.println("4 Alle 3 samtidig");
	}
	else{
	    Oblig1 test = new Oblig1();
	    typeSortering=Integer.parseInt(args[0]);
	    test.sorter();
	}
    } //end main
    int[] generate(int size){
	int[] generated = new int[size];
	Random r = new Random(1337);
	for (int i=0;i<size;i++){
	    generated[i]=r.nextInt();
	}
	return generated;
    }
    int[] innstikkSort(int[] a){
	int j;  //antallet sorterte så langt
	int key; //tallet som skal settes inn
	int i,l;
	for (j=1;j<40;j++){ //antallet tall som er ferdigsortert
	    key=a[j];
	    for (i=j-1;(i>=0)&&(a[i]<key);i--){ //flytter opp de små verdiene
		a[i+1]=a[i];
	    }
	    a[i+1]=key; //
	}
	for (l=40;l<a.length;l++){ // går gjennom array og sjekker hvert tall
	    if (a[l]>a[39]){
		settInn40(a,l,39);
	    }
	}
	return a;
    }
    // sorterer de første 40 tallene i arrayen som skal parallellsorteres
    int[] innstikkSortPara(int[] p){
	int j;  //antallet sorterte så langt
	int key; //tallet som skal settes inn
	int i,l;
	for (j=1;j<40;j++){ //antallet tall som er ferdigsortert
	    key=p[j];
	    for (i=j-1;(i>=0)&&(p[i]<key);i--){ //flytter opp de små verdiene
		p[i+1]=p[i];
	    }
	    p[i+1]=key; //
	}
	return p;
    }       
    void settInn40(int[] s,int posisjon,int start){
	int temp=s[start];
	s[start]=s[posisjon];
	s[posisjon]=temp;
	if(start>0){
	    if (s[start]>s[start-1]){
		settInn40(s,start,(start-1));
	    }
	}
    } // end settInn40
    synchronized void settInnPara(int[] s,int posisjon,int start){
	int temp=s[start];
	s[start]=s[posisjon];
	s[posisjon]=temp;
	if(start>0){
	    if (s[start]>s[start-1]){
		settInn40(s,start,(start-1));
	    }
	}
    } // end settInnPara
    int antCores(){
	return antKjerner;
    }
    void sorter(){
	antTraader=antCores();
	b = new CyclicBarrier(antTraader+1); 
	System.out.println("\n"+"Sorterer arrayer i forskjellige stoerrelser");
	for (int i=0;i<arraySize.length;i++){ 
	    int size = arraySize[i];
	    System.out.println((i+1)+". Array "+"\t"+arraySize[i]+" tall");
	    for (int j=0;j<numIter;j++){ 
		// Printer litt informasjon om hva programmet gjør
		if (j==0){
		    System.out.print("Iterasjon ");
		}
		if (j==(numIter-1)){
		    System.out.println(j+1);
		}
		else{
		    System.out.print(j+1);
		}

		long time;

		// Sorterer s1 med Arrays.sort()
		if (typeSortering==1 || typeSortering==4){
		    s1=generate(size);
		    time=System.nanoTime();
		    Arrays.sort(s1);
		    time=(System.nanoTime()-time);
		    innebygd[j]=time; 
		}
		// Sorterer s2 sekvensielt
		if (typeSortering==2 || typeSortering==4){
		    s2=generate(size);
		    time=System.nanoTime();		
		    innstikkSort(s2); //skal sorteres 40 først
		    time=(System.nanoTime()-time);
		    sekvensiell[j]=time; 
		}
		// Sorterer s3 parallelt
		if (typeSortering==3 || typeSortering==4){
		    s3=generate(size);
		    // sorterer 40 første tallene
		    //innstikkSortPara(s3);
		    time=System.nanoTime();

		    // Starter opp tråder for parallellsortering
		    for (int k=0;k<antTraader;k++){
			new Thread(new Para(k)).start();
		    }
		    try{ // venter på barrieren b
			b.await();
		    } catch (Exception e) {return;}

		    time=(System.nanoTime()-time);
		    parallell[j]=time; 	
		}
	    } //end for j
	    // Regner ut medianverdiene og lagrer dem i array
	    Arrays.sort(innebygd);
	    Arrays.sort(sekvensiell);
	    Arrays.sort(parallell);
	    // Lagrer medianverdiene siden numIter=9 er dette 5. 
	    // element i en sortert array (synkende eller stigende)
	    innebygdM[i]=innebygd[4];
	    sekvensiellM[i]=sekvensiell[4];
	    parallellM[i]=parallell[4];
	} // end for i
	// Skriver ut medianverdiene
	System.out.println();
	System.out.println("\t"+"  Innebygd"+"\t"+"Sekvensiell"+"\t"+"Parallell");
	for (int iq=0;iq<arraySize.length;iq++){
	    System.out.println("Medianen: "+innebygdM[iq]+"\t"+sekvensiellM[iq]+
			       "\t"+"\t"+parallellM[iq]);
	}
	//Sjekker om arrayene er sortert korrekt
	if (typeSortering==4){
	    testMetode();
	}
    } // end sorter
    void testMetode(){
	System.out.print("\n"+"Tester om arrayene er like: "+"\n");
	// check to see if they are equal
	// bytt ut s3 med s2 i if statement for å sjekke denne
	// ok sorterer jo ikke hele, bare begynnelsen.....
	boolean testme=true;
	int xss=s2.length-1;
	int correct=0;
	for (int as=0;as<40;as++){
	    if (s3[as]==(s1[xss])){
		testme=true;
		correct++;
	    }
	    else{
		testme=false;
		System.out.println("I have found a wrong number");
	    }
	    xss--;
	}
	System.out.println("The arrays are equal: "+testme);
	System.out.println("Antall like tall: "+correct+"\n");
    }
    class Para implements Runnable{
	int ind;
	Para(int i){
	    ind=i;
	}
	public void run(){
		// deler opp arrayen i biter
		// noen tall er allerede sortert
		int ant = ((s3.length-40)/antTraader);
		int num = ant;
		// siste tråden får eventuelt litt flere tall
		if (ind == antTraader-1){
		    num = ant + ((s3.length-40)%antTraader);
		}
		for (int i=0;i<num;i++){
		    if (s3[40+(ant*ind+i)]>s3[39]){
			settInnPara(s3,40+(ant*ind+i),39);
		    }
		}
	    	try{
	    	    b.await();
	    	} catch (Exception e) {return;}
	} // end run
    } // end para
} // end 

