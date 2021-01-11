import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*; //reentrantlock

class Oblig3{
    static int n;
    final static int NUM_BIT = 7;
    int antTraader,antKjerner,numIter;
    CyclicBarrier vent,synk;
    double[] sekvensiellTid,parallellTid,iterasjonsTid;
    int[] a;
    int globalMax;
    ReentrantLock lock;
    int[][] allCount;
    int[] sumCount,sumUnder;

    Oblig3(){
	this.n = n;
	antKjerner = Runtime.getRuntime().availableProcessors();
	antTraader = antKjerner;
	vent   = new CyclicBarrier(antTraader+1); //inkludert main
	numIter=3; // antall iterasjoner tar lang tid for store antall
	lock=new ReentrantLock();
	synk = new CyclicBarrier(antTraader);
	allCount=new int[antTraader][];
	sumUnder=new int[antTraader];
    }
    public static void main(String[] args){
	if (args.length != 1){
	    System.out.println("Use java Oblig3 <number>");
	    System.exit(0);
	}
	else{
	    n = Integer.parseInt(args[0]);
	}
	Oblig3 program = new Oblig3();
	program.start();
    }
    void start(){
	// Beregner sekvensiell tidsforbruk først:
	System.out.println("\n"+"Genererer int array og sorterer med Sekvensiell Radix: ");
	System.out.println("Antall tall "+"\t"+" Tidsforbruk "); 
	System.out.print(n+"      "+"\t");
	iterasjonsTid = new double[numIter];
	for (int j=0;j<numIter;j++){
	    a = generate(n);
	    long t = System.nanoTime();
	    a=radixMulti(a);
	    double tid = (System.nanoTime()-t)/1000000.0;
	    iterasjonsTid[j] = tid;
	}
	// antar numIter=3
	Arrays.sort(iterasjonsTid);
	System.out.println(iterasjonsTid[1]+" ms"); // median av sortert array
	testSort(a); // sjekker om arrayen er sortert riktig, tar litt tid...

	// Sorterer Parallelt
	System.out.println("\n"+"Genererer int array og sorterer med parallell Radix: ");
	System.out.println("Antall tall "+"\t"+" Tidsforbruk "); 
	System.out.print(n+"      "+"\t");
	iterasjonsTid = new double[numIter];
	for (int j=0;j<numIter;j++){
	    a = generate(n);
	    long tt = System.nanoTime();
	    for (int k=0;k<antTraader;k++){
		new Thread(new Para(k)).start();
	    }	
	    try{
		vent.await(); // starter trådene
		// alle trådene er nå ferdige og har fått global max
	    } catch (Exception e) {return;}
	    a=radixMultiPara(a);
	    

	    double tid = (System.nanoTime() -tt)/1000000.0;
	    iterasjonsTid[j] = tid;
	} // end iterations
	Arrays.sort(iterasjonsTid);
	System.out.println(iterasjonsTid[1]+" ms"); // median av sortert array
	testSort(a); // sjekker om arrayen er sortert riktig, tar litt tid...
    } // end main
 
   // Generates a random int array
    int[] generate(int size){
	int[] generated = new int[size];
	Random r = new Random(1337);
	for (int i=0;i<size;i++){
	    // in order to get positive numbers??
	    generated[i]=r.nextInt(size-1); // mellom 0 og n-1
	}
	return generated;
    }
    void updateMax(int t){
    	lock.lock();
    	if (t>globalMax){
    	    globalMax=t;
    	}
    	lock.unlock();
    }

    class Para implements Runnable{
	int ind,fra,til,num; // thread id
	int tempMax;
	Para(int in){
	    ind = in;
	}
	void paraInitialize(int n){
	    num=n/antTraader;
	    fra=num*ind;
	    til=(ind+1)*num;
	    if (ind == antTraader-1){
		til = n;
	    }
	}
	public void run(){
	    paraInitialize(n);	    
	    for (int i=fra;i<til;i++){
		if (a[i]>tempMax){
		    tempMax=a[i];
		}
	    }
	    updateMax(tempMax);
	    try{
		vent.await();
	    } catch (Exception e) {return;}
	}
    } // end Para class

    // kode hentet fra obligtekst/hjemmeside
    int []  radixMulti(int [] a) {
	//long tt = System.nanoTime();
	// 1-5 digit radixSort of : a[]
	int max = a[0], numBit = 2, numDigits, n =a.length;
	int [] bit ;
	
	// a) finn max verdi i a[]
	for (int i = 1 ; i < n ; i++)
	    if (a[i] > max) max = a[i];
	while (max >= (1L<<numBit) )numBit++; // antall siffer i max
	// bestem antall bit i numBits sifre
	numDigits = Math.max(1, numBit/NUM_BIT);
	bit = new int[numDigits];
	int rest = (numBit%numDigits), sum =0;;
	
	// fordel bitene vi skal sortere paa jevnt
	for (int i = 0; i < bit.length; i++){
	    bit[i] = numBit/numDigits;
	    if ( rest-- > 0)  bit[i]++;
	}
	int[] t=a, b = new int [n];
	for (int i =0; i < bit.length; i++) {
	    radixSort( a,b,bit[i],sum );    // i-te siffer fra a[] til b[]
	    sum += bit[i];
	    // swap arrays (pointers only)
	    t = a;
	    a = b;
	    b = t;
	}
	if (bit.length%2 != 0 ) {
	    // et odde antall sifre, kopier innhold tilbake til original a[] (n� b)
	    System.arraycopy (a,0,b,0,a.length);
	}
	return a;
    } // end radixMulti
    int[] radixMultiPara(int[] a){
	// 1-5 digit radixSort of : a[]
	int numBit = 2, numDigits, n =a.length;
	int [] bit ;
	while (globalMax >= (1L<<numBit) )numBit++; // antall siffer i max
	
	// bestem antall bit i numBits sifre
	numDigits = Math.max(1, numBit/NUM_BIT);
	bit = new int[numDigits];
	int rest = (numBit%numDigits), sum =0;;
	
	// fordel bitene vi skal sortere paa jevnt
	for (int i = 0; i < bit.length; i++){
	    bit[i] = numBit/numDigits;
	    if ( rest-- > 0)  bit[i]++;
	}
	int[] t=a, b = new int [n];
	for (int i =0; i < bit.length; i++) {
	    radixSortPara( a,b,bit[i],sum );    // i-te siffer fra a[] til b[]
	    sum += bit[i];
	    // swap arrays (pointers only)
	    t = a;
	    a = b;
	    b = t;
	}
	if (bit.length%2 != 0 ) {
	    // et odde antall sifre, kopier innhold tilbake til original a[] (n� b)
	    System.arraycopy (a,0,b,0,a.length);
	}
	return a;
    }

    /** Sort a[] on one digit ; number of bits = maskLen, shiftet up 'shift' bits */
    void radixSort ( int [] a, int [] b, int maskLen, int shift){
	//System.out.println(" radixSort maskLen:"+maskLen+", shift :"+shift);
	int  acumVal = 0, j, n = a.length;
	int mask = (1<<maskLen) -1;
	int [] count = new int [mask+1];
	// b) count=the frequency of each radix value in a
	for (int i = 0; i < n; i++) {
	    count[(a[i]>>> shift) & mask]++;
	}
	// c) Add up in 'count' - accumulated values
	for (int i = 0; i <= mask; i++) {
	    j = count[i];
	    count[i] = acumVal;
	    acumVal += j;
	}
	// d) move numbers in sorted order a to b
	for (int i = 0; i < n; i++) {
	    b[count[(a[i]>>>shift) & mask]++] = a[i];
	}
    }// end radixSort
    void radixSortPara(int[] a,int[] b,int maskLen,int shift){
	int  acumVal = 0, j, n = a.length;
	int mask = (1<<maskLen) -1;
	sumCount = new int [mask+1];

	for (int k=0;k<antTraader;k++){
	    new Thread(new Para4(k,mask,shift,sumCount,b)).start();
	}	
	try{
	    vent.await(); // starter trådene
	} catch (Exception e) {return;}

	// kommenter ut denne for å sjekke Para4 run metode
	// koden i para4 fungerer hvis denne kjøres sekvensielt.
	// d) move numbers in sorted order a to b
	for (int i = 0; i < n; i++) {
	  b[sumCount[(a[i]>>>shift) & mask]++] = a[i];
	}
    }
    class Para4 implements Runnable{
	int ind,fra,til,num; // thread id
	int mask,shift,sum2,totalSum;
	int[] localCount,sumCount,b;
	Para4(int in,int mask,int shift,int[] sumCount,int[] b){
	    this.mask=mask;
	    this.shift=shift;
	    this.sumCount=sumCount;
	    this.b=b;
	    ind = in;
	}
	void paraInitialize(int n){
	    num=n/antTraader;
	    fra=num*ind;
	    til=(ind+1)*num;
	    if (ind == antTraader-1){
		til = n;
	    }
	    localCount=new int[mask+1];
	}
	public void run(){
	    paraInitialize(n);

	    // parallelliserer b)
	    for (int i=fra;i<til;i++){
		localCount[(a[i]>>> shift) & mask]++;
	    }
	    allCount[ind]=localCount; // henger opp i double int array
	    try{
		synk.await();
	    } catch (Exception e) {return;}
	    // deler opp array
	    int num2=(sumCount.length)/antTraader;
	    int fra2=num2*ind;
	    int til2=(ind+1)*num2;
	    if (ind==(antTraader-1)){
		til2=sumCount.length;
	    }
	    for (int j=fra2;j<til2;j++){
		sum2=0;
		for (int m=0;m<antTraader;m++){
		    sum2 += allCount[m][j];
		}
		sumCount[j]=sum2;
		totalSum += sum2; // hvor mange i min del. se c)
	    }
	    sumUnder[ind]=totalSum;

	    try{
		synk.await();
	    } catch (Exception e) {return;}
	    
	    // parallelliserer c)
	    // Summerer opp i sumCount[]  akkumulerte verdier (pekere)
	    int antallUnder=0;
	    for (int n=0;n<ind;n++){
		antallUnder += sumUnder[n];
	    }
	    int j1=0;
	    int num3=(mask+1)/antTraader;
	    int fra3=num3*ind;
	    int til3=(ind+1)*num3;
	    if (ind==(antTraader-1)){
		til3=mask+1;
	    }	    
	    for (int o = fra3; o<til3; o++) {
		j1 = sumCount[o];
		sumCount[o] = antallUnder;
		antallUnder += j1;
	    }	  	    

	    try{
		synk.await();
	    } catch(Exception e) {return;}
	    
	    /*
	    // Fungerer ikke. Får arrayindexout of bounds exception på b
	    if (ind==0){
		//System.out.println("flytter fra a til b");
		for (int k1=0;k1<n;k1++){
		    b[sumCount[(a[k1]>>> shift) & mask]++] = a[k1];		    
		}
	    }
	    */

	    /* 
	    // Prøver å parallellisere d), men fikk det ikke til
	    // Fungerer for n=2000, men får arrayindexoutofbounds på 20000+

	    // Skal flytte fra a til b
	    // Prøver å gå gjennom hele a[] og plukke ut trådens sifferverdier
	    // som så settes inn i b. Burde vel virke i teorien??
	    // SumCount inneholder info om hvor mange siffer som er plassert under
	    for (int i=0;i<n;i++){
		int t1=((a[i]>>> shift) & mask);
		if (t1>=fra3){ 
		    if (t1<til3){
			b[sumCount[(a[i]>>> shift) & mask]++] = a[i];
		    }
		}
	    }
	    */

	    try{
		vent.await();
	    } catch (Exception e) {return;}
	} // end run
    } // end Para class

    void testSort(int[] a){
	for (int i=0;i<a.length-1;i++){
	    if (a[i]>a[i+1]){
		System.out.println("SorteringsFEIL på plass: "+
				   i +" a["+i+"]:"+a[i]+" >a["+(i+1)+"]:"+a[i+1]);
		return;
	    }
	}
    } // end testSort
}