package main;

public class Main{
    public static void main(String[] args){
        PetriNet pnet = new PetriNet
            (
             "./includes/prod_cons/incidense.csv",
             "./includes/prod_cons/marking.csv",
             "",
             ""
             );
        Monitor mon = new Monitor(pnet);
        mon.stopAfterTransitionsFired(100);
        mon.setVerboseLevel(1);

        Task prod = new Task(new int[]{0, 2}, pnet, mon);
        Task cons = new Task(new int[]{1, 3}, pnet, mon);

        Thread th_prod = new Thread(prod, "ProdThread");
        Thread th_cons = new Thread(cons, "ConsThread");

        th_prod.start();
        th_cons.start();

        try{
            th_prod.join();
            th_cons.join();
        } catch (InterruptedException e){
            e.printStackTrace();
        }
    }
}
