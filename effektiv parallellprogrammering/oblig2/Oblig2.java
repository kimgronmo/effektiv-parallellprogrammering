import java.util.*;
import java.util.concurrent.*; // cyclicbarrier
import java.util.concurrent.locks.*;

class Oblig2{
    static int maxNum;
    byte[] bitArr,bitArrSmall;
    final int [] bitMask ={1,2,4,8,16,32,64};
    final int [] bitMask2 ={255-1,255-2,255-4,255-8,255-16,255-32,255-64};
    long[] factor;
    CyclicBarrier wait,ready;
    int antKjerner,antTraader;
    int[] crossOutTable; // liten tabell som skal avkrysses fra
    //ReentrantLock lock;
    int rest; // rest når man faktoriserer parallelt
    long remaining; // rest når man faktoriserer parallelt

    Oblig2(int maxNum){
	this.maxNum = maxNum;
	antKjerner = Runtime.getRuntime().availableProcessors();
	crossOutTable=new int[(int)Math.sqrt(maxNum)];
	//lock=new ReentrantLock();
	//antKjerner=4;
	int rest=0;
    }
    public static void main(String args[]){
	if (args.length != 1){
	    System.out.println("Use java Oblig2 <number>");
	}
	else{
	    maxNum = Integer.parseInt(args[0]);
	    if (maxNum<=16){
		System.out.println("\n"+"Use a number larger than 16");
		System.exit(0);
	    }
	}
	Oblig2 oblig2 = new Oblig2(maxNum);
	oblig2.start();
    }
    void start(){
        generatePrimesSeq(maxNum);
	factoriseSeq(); // sjekk hvor jeg skal ta tidtakinga
	generatePrimesPara(maxNum); // må sende med array for å sjekke neste osv korrekt
	//System.out.println(isPrime(5001,bitArr));
	factorisePara();


    }
    void generatePrimesSeq(int maxNum){
	bitArr = new byte[(maxNum/14)+1]; //+1 hvis tall til overs
	setAllPrime(bitArr); // Set all bytes to have only 1's
	double t=System.nanoTime();
	int p=3;
	eratosthenesSieve(p,bitArr,maxNum); // starts at number 3
	boolean tester=hasNextPrime(p,bitArr);
	while (tester){
	    p=nextPrime(p,bitArr);
	    eratosthenesSieve(p,bitArr,maxNum);
	    // trenger ikke krysse av for tallene over roten av maxNum
	    // sparer mye tid på dette
	    if (p>Math.sqrt(maxNum)){
		tester=false;
	    }
	}
	t=(System.nanoTime()-t)/1000000.0;
	System.out.println("Tidsforbruk sekvensielt er nå: "+t+"ms");
    }
    void generatePrimesPara(int maxNum){
	// hele lange listen av primtall
	bitArr = new byte[(maxNum/14)+1]; //+1 hvis tall til overs
	bitArrSmall = new byte[((int)Math.sqrt(maxNum)/14)+1];
	int smallSize = (int)Math.sqrt(maxNum);
	//System.out.println(smallSize);
	setAllPrime(bitArr); // Set all bytes to have only 1's
	setAllPrime(bitArrSmall);
	// Makes a small table of numbers 3-->sqrt(maxNum)
	// only numbers we need to cross out for
	// then divide these numbers among the threads
	int maxNumOld=maxNum;
	maxNum=smallSize;
	double t=System.nanoTime();
	int p=3;
	eratosthenesSieve(p,bitArrSmall,maxNum); // starts at number 3
	//System.out.println("kom jeg hit");
	boolean tester=hasNextPrime(p,bitArrSmall);
	//boolean tester=true;
	//System.out.println("smallsize er: "+smallSize);
	while (tester){
	    p=nextPrime(p,bitArrSmall);
	    eratosthenesSieve(p,bitArrSmall,maxNum);
	    // trenger ikke krysse av for tallene over roten av maxNum
	    // sparer mye tid på dette
	    if (p>Math.sqrt(maxNum)){ //maxNum????
	    	tester=false;
	    }
	    //else{ // droppe denne??
	    //tester=hasNextPrime(p,bitArrSmall);
	    //}
	}
	// har vel nå en small array med korrekte primes?
	// gjøre denne til int array og fordele blant traader
	// som skal krysse av i store tabellen.
	antTraader=antKjerner;
	wait= new CyclicBarrier(antTraader+1);
	ready= new CyclicBarrier(antTraader+1);
	ArrayList<Integer> list = new ArrayList<Integer>();

	int x2=2;
	boolean me=hasNextPrime(x2,bitArrSmall);
	while (me){
	    x2=nextPrime(x2,bitArrSmall);

	    if (x2>=smallSize) break; //hvilke skal vi krysse av? mindre enn sqrt(N)31x31=961
	    //System.out.println("kommer jeg meg hit sist");
	    list.add(x2);

	    //System.out.println(x2);
	    me=hasNextPrime(x2,bitArrSmall);
	}
	int length=list.size();
	//System.out.println(length);
	crossOutTable=new int[length];
	//crossOutTable=list.toIntArray();
	Iterator<Integer> iterate=list.iterator();
	int a=0;
	while (iterate.hasNext()){
	    crossOutTable[a]=(int)iterate.next();
	    //System.out.println(crossOutTable[a]);
	    a++;
	}
	maxNum=maxNumOld;


	// Starter opp tråder for parallellsortering
	for (int k=0;k<antTraader;k++){
	    new Thread(new Para(k,crossOutTable,bitArr)).start();
	}
	try{ // venter på barrieren b
	    wait.await();
	} catch (Exception e) {return;}


	t=(System.nanoTime()-t)/1000000.0;
	System.out.println("Tidsforbruk parallelt er nå: "+t+"ms");





	// må sjekke om jeg ikke resetter gamle liste tilbake igjen for 
	// hvert tall. eller glemmer å krysse av
	// denne kan tas vekk sjekker om jeg får riktig antall primes
	// til slutt
	int t1=2;
	int counter=1;
	while(hasNextPrime(t1,bitArr)){ // denne kan bli messy når jeg må sende med array
	    counter++;
	    t1=nextPrime(t1,bitArr);
	    //System.out.println("prime er: "+t1);
	}
	System.out.println("number of primes are: "+counter);





    }
    // set all odd numbers as prime numbers
    // sjekk om denne gjør rett for liten array også
    void setAllPrime(byte[] bitArr){
	for (int i=0;i<bitArr.length;i++){
	    bitArr[i]=(byte)127;
	}
	// sets 1 to be a non prime
	bitArr[0] &= ~(1<<0); // set bit to 0
    }
    // needs to get array to cross out.
    void eratosthenesSieve(int start,byte[] bitArr,int maxNum){
	// 2 is a prime number starts at 3
	int p=start;
	int t2=2;
	long tmp= (long) p*(long)p;
	int temp=p*p;
	while (tmp<(maxNum)){
	    crossOut(temp,bitArr);
	    temp=p*p+p*t2;
	    tmp=(long)p*(long)p+(long)p*(long)t2;
	    t2=t2+2;
	}
    }
    // må sende med array i alle disse metodene ellers blir det bare feil


    // needs to get array to cross out. need only one method then
    // synchro void, prøver å gjøre noe med samme bye problemer
    // lag en synch metode som henter byten.
    // lock på byten??
    // synch hent byte, lås den, kryss ut lås opp...

    void crossOut(int i,byte[] bitArr){
	//lock.lock();

	bitArr[i/14] &= bitMask2[(i%14)>>1]; 
	//try{
	//    lock.unlock();
	//} catch (Exception e) {return;}


    }

    // send med array
    boolean hasNextPrime(int sjekk,byte[] bitArr){
	if (sjekk<1){
	    System.out.println("ikke her det går galt ");
	}
	if (sjekk>=maxNum){
	    return false;
	}
	else{
	    int lint=0;
	    lint =nextPrime(sjekk,bitArr);
	    if (isPrime(lint,bitArr)){
		return true;
	    }
	    else{
		return false;
	    }
	}
    }
    // send videre array
    int nextPrime(int i,byte[] bitArr){
	// returns next prime number after number 'i'
	int k=0 ;
	if ((i&1)==0){
	    k =i+1; // if i is even, start at i+1
	}
	else{
	    k = i+2; // next possible prime
	}
	while (!isPrime(k,bitArr)){
	    if (k>maxNum) break; //break fjerner vel bare loopen ikke hoppe ut helt?
	    k=k+2; // prøver å sjekke for 101
	}
	return k;
    } // end nextPrime
    // motta array
    boolean isPrime (int i,byte[] bitArr) {
	if (i>maxNum){
	    return false; 
	}
	if (i<=0){
	    return false;
	}
	if (i == 2 ){ 
	    return true;
	}
	if ((i&1) == 0){
	    return false; // 0 i siste bit dvs. partall
	}
	else{ 
	    //System.out.println("sjekker tallet i: "+i);
	    return (bitArr[i/14] & bitMask[(i%14)>>1]) != 0;
	}
    }
    void factoriseSeq(){
	// finds 100 numbers lower than maxNum*maxNum
	// assumes we start at number 1
	if (maxNum<100){
	    System.out.println("You need a higher number to factorise enough");
	    System.exit(0);
	}
	factor=new long[100];
	for (int i=1;i<101;i++){
	    factor[100-i]=(long)maxNum*(long)maxNum-i;
	}
	ArrayList<ArrayList> allNumbers = new ArrayList<ArrayList>();
	double totalTid=System.nanoTime();
	// lag en liste med arrayer her og ta tiden fra start til slutt for 100 tall
	for (int j=0;j<factor.length;j++){
	    int teller=2;
	    ArrayList<Long> array = new ArrayList<Long>();
	    long factorMe=factor[j];
	    boolean tester=true;
	    //System.out.print("Faktoriserer tallet: "+factorMe+" ");
	    double t2 =System.nanoTime();

	    // må sende med korrekt array her for å sjekke nextPrime osv
	    while(tester){ //sjekke å dele med seg selv
		while(factorMe%teller==0){ //delbar
		    array.add((long)teller);
		    factorMe=factorMe/teller;
		}
		if (factorMe==1){ //fjern denne??
		    tester=false; // sjekk om faktorisering blir riktig
		}
		if (teller>=(Math.sqrt(factorMe))){
		    if (tester){
			array.add(factorMe);
			tester=false;
		    }
		}
		if (hasNextPrime(teller,bitArr)){
		    teller=nextPrime(teller,bitArr);
		}
		else{ // denne som crasher hvis den bare er delelig på seg selv.
		    if (factorMe>maxNum){
			array.add(factorMe);
			tester=false;
		    }
		    else{
			if (factorMe==maxNum){
			    array.add(factorMe); // siste tallet er et primtall
			}
			tester=false; // ferdig med å dele på alle tall
		    }
		}

	    }
	    t2=(System.nanoTime()-t2)/1000000.0;
	    //System.out.println("tid brukt er "+t2+" ms");
	    allNumbers.add(array);
	}
	// tid her er inkludert printing??
	totalTid=(System.nanoTime()-totalTid)/1000000.0;
	System.out.println("Total tid brukt er "+totalTid);
	printPrimes(allNumbers);
    }
    void printPrimes(ArrayList<ArrayList> done){
	for (int i=0;i<5;i++){
	    System.out.print(factor[i]+" = "+done.get(i).get(0));
	    for (int j=1;j<done.get(i).size();j++){
		System.out.print("*"+done.get(i).get(j));
	    }
	    System.out.println(); // neste linje
	}
	for (int i=95;i<100;i++){
	    System.out.print(factor[i]+" = "+done.get(i).get(0));
	    for (int j=1;j<done.get(i).size();j++){
		System.out.print("*"+done.get(i).get(j));
	    }
	    System.out.println(); // neste linje
	}
	int t=2;
	int counter=1;
	while(hasNextPrime(t,bitArr)){ // denne kan bli messy når jeg må sende med array
	    counter++;
	    t=nextPrime(t,bitArr);
	}
	System.out.println("number of primes are: "+counter);
    }
    void testMe(){
	int[] maxtall= {100,1000,10000,100000,1000000,10000000,100000000,1000000000};
	int[] fasit= {25,168,1229,9592,78498,664579,5761455,50847534};
    }
    class Para implements Runnable{
	int ind;
	int[] crossOutTable;
	byte[] bitArray;

	Para(int i,int[] crossOutTable,byte[] bitArray){
	    ind=i; // hvilken tråd
	    this.crossOutTable=crossOutTable;
	    this.bitArray=bitArray;

	}
	public void run(){
	    int bytesPerTraad=(maxNum/14)/antTraader; // fordeler bytes blant trådene
	    int bytesPerTraadLast=0;
	    
	    if (ind==antTraader-1){
		if ((maxNum/14)%antTraader!=0){
		    int rest=maxNum - bytesPerTraad*antTraader*14;
		    //System.out.println("rest er: "+rest);
		    if (rest%14!=0){
			bytesPerTraadLast=bytesPerTraad+(rest/14)+1;
		    }
		    else{
			bytesPerTraadLast=bytesPerTraad+(rest/14);
		    }
		}
	    }
	    //System.out.println("Antall bytes per traad er: "+bytesPerTraad);
	    int startTall=ind*bytesPerTraad*14+1;
	    int sluttTall;
	    if (ind==antTraader-1){
		sluttTall=ind*bytesPerTraad*14+14*bytesPerTraadLast;
	    }
	    else{
		sluttTall=ind*bytesPerTraad*14+14*bytesPerTraad;
	    }
	    //System.out.println("startTall er: "+startTall+" sluttTall er: "+sluttTall);

	    for (int i=0;i<crossOutTable.length;i++){
		int p=crossOutTable[i];
		int t2=2;
		long tmp=(long)p*(long)p;
		int temp=p*p;
		while(tmp<maxNum){
		    if (temp>=startTall && temp<= sluttTall){
			crossOut(temp,bitArr);
			//if (temp==5001){
			//    System.out.println("krysset av for: "+temp);
			//}

		    }
		    temp=p*p+p*t2;
		    if (temp>sluttTall){
			break; // trenger ikke krysse av i større bytes
		    }
		    tmp=(long)p*(long)p+(long)p*(long)t2;
		    t2=t2+2;
		}
	    }
	    // starte tråden som lager erathostenes, vente og fortsette når den skal faktorisere?
	    //int ant=crossOutTable.length/antTraader;
	    //int num=ant;
	    //if (ind==antTraader-1){
		//num=ant+crossOutTable.length%antTraader;
		//}
	    //for (int i=0;i<num;i++){
		//eratosthenesSieve(crossOutTable[ant*ind+i],bitArray,maxNum);
		//}
	    try{
		wait.await();
	    } catch (Exception e) {return;}
	    //System.out.println("Tester traaden");
	} // end run
    } // end para
    void factorisePara(){
	// finds 100 numbers lower than maxNum*maxNum
	// assumes we start at number 1
	if (maxNum<100){
	    System.out.println("You need a higher number to factorise enough");
	    System.exit(0);
	}
	factor=new long[100];
	for (int i=1;i<101;i++){
	    factor[100-i]=(long)maxNum*(long)maxNum-i;
	}
	ArrayList<ArrayList> allNumbers2 = new ArrayList<ArrayList>();
	double totalTid=System.nanoTime();

	// Starter opp tråder for parallellsortering
	for (int k=0;k<factor.length;k++){
	    long factorMe=factor[k];
	    setRem(factorMe);
	    //System.out.println("remaining er: "+remaining);
	    ArrayList<Long> array = new ArrayList<Long>();
	    double test=System.nanoTime();
	    for (int j=0;j<antTraader;j++){
		new Thread(new ParaFactorise(j,array,factorMe)).start();
	    }
	    try{
		ready.await();
	    } catch (Exception e) {return;}
		       // sjekk om det er tall større en maxnum
	    //System.out.println("tråd nummer: "+k+"array lengde "+array.size());
	    //Iterator iterate = array.iterator();
	    test=(System.nanoTime()-test)/1000000.0;
	    //System.out.println("Traad brukte: "+test+" ms");
	    //while (iterate.hasNext()){
	    //long temp=(long)iterate.next();
	    //	remaining=remaining/temp;
	    //}
	    //if (remaining>maxNum){ // if big prime
	    //array.add(remaining);
	    //}
	    //System.out.println(remaining);
	    if (remaining!=1){
		array.add(remaining);
	    }


		       
	    allNumbers2.add(array);
		       
	}
	//try{ // venter på barrieren b
	//    ready.await();
	//} catch (Exception e) {return;}
	totalTid=(System.nanoTime()-totalTid)/1000000.0;
	System.out.println("Tidsforbruk parallelt er nå: "+totalTid+"ms");	
	printPrimes(allNumbers2);    


    }
    //synchronized void updateRemaining(long rem){
    //	remaining=remaining/rem;
    //}
    void setRem(long rem){
	remaining=rem;
	//System.out.println("jeg har tilgang til denne "+remaining);
    }
    synchronized void remain(long rem){
	remaining=remaining/rem;
    }
    synchronized void addToArray(ArrayList<Long> array,long number){
	array.add(number);
    }

    class ParaFactorise implements Runnable {
	//boolean last;
	long remainder;
	int ind,startTall,sluttTall;
	ArrayList<Long> tempArray;
	

	ParaFactorise(int i,ArrayList<Long> array,long factorMe){
	    //last=false;
	    remainder=factorMe;
	    ind=i; // traad nummer
	    tempArray=array;
	}
	public void run(){
	    //System.out.println("jeg starter tråd: "+ind+" skal faktorisere: "+remainder);

	    int num=maxNum/antTraader;
	    startTall=ind*num + 1;
	    sluttTall=(ind+1)*num;
	    if (ind==0){
		startTall=2;
	    }
	    
	    if (!isPrime(startTall,bitArr)){
		//System.out.println("tallet: "+startTall+" er ikke prime");
		startTall=nextPrime(startTall,bitArr);
	    }
	    
	    if (ind==(antTraader-1)){
		sluttTall=maxNum;
	    }
	    int teller=startTall;
	    boolean tester=true;
	    //here();
	    //System.out.println("ind er "+ind+" remaining er "+antKjerner);
	    while(tester){
		remainder=remaining;
		while((remainder%teller)==0){ // delbar
		    //tempArray.add((long)teller);
		    addToArray(tempArray,(long)teller);


		    //System.out.println("Found a number"+teller);
		    remain((long)teller);
		    //remaining=remaining/teller;
		    //System.out.println(remaining);
		    remainder=remaining;
		}
		if (remainder==1){
		    tester=false; //break denne?
		    //break;
		}
		if (teller>=(Math.sqrt(remainder))){
		    if (tester){
			if (remainder>=startTall && remainder<=sluttTall){
			    //System.out.println("legger til tallet: "+remainder+" traad: "+ind);
			    addToArray(tempArray,remainder);
			    remain(remainder);

			}
			tester=false;
		    }
		}


		if (hasNextPrime(teller,bitArr)){
		    teller=nextPrime(teller,bitArr);
		    if (teller>sluttTall){
			tester=false;
		    }
		    if (remainder<teller){
			tester=false;
		    }
		    if (teller>remainder){
			tester=false;
		    }

		}
		else{ // no more primes to check
		    tester=false;
		}
	    }

	    try{
		ready.await();
	    } catch (Exception e) {return;}
	    
	}
    }
}	




