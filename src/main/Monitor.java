package main;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.assertTrue;

public class Monitor{
    private PetriNet PNet;
    private int count;
    private int verbose;
    private Runnable op;
    private Condition[] ConditionQueue;

    private ReentrantLock lock;

    public Monitor(PetriNet PNet){
        this.PNet = PNet;
        this.count = 0;
        this.op = () -> {};
        this.lock = new ReentrantLock();
        this.verbose = 0;

        this.ConditionQueue = new Condition[this.PNet.getAmountTransitions()];
        for(int i = 0; i < this.PNet.getAmountTransitions(); i++)
            this.ConditionQueue[i] = lock.newCondition();

        // this.CourtesyQueue = lock.newCondition();
    }

    public void stopAfterTransitionsFired(int number){
        this.count = number;
        this.op = () -> this.count--;
    }

    public void setVerboseLevel(int lvl){
        this.verbose = lvl;
    }

    public boolean exec(Transition t){
        // Monitor simple, usa "signal and continue"
        // no tiene cola de cortesia, los threads que
        // reciben signal tienen que pelear por el lock
        // con los threads de la cola de entrada
        this.lock.lock();
        try{
            Transition next_t;

            while(!this.PNet.isReady(t)){
                if((next_t = this.PNet.getNextTransition()) != null){
                    this.ConditionQueue[next_t.getId()].signal();
                }
                try{
                    this.ConditionQueue[t.getId()].await
                        (this.PNet.getRemainingTime(t), TimeUnit.MILLISECONDS);
                } catch (InterruptedException e){
                    if(this.verbose > 1)
                        System.out.println
                            ("Msg From: "+Thread.currentThread().getName()+
                             "\nThread interrumpido");
                    return false;
                }
            }
            this.op.run();
            if(this.count < 0){
                if(this.verbose > 1)
                    System.out.println
                        ("Msg From: "+Thread.currentThread().getName()+
                         "\nInterrumpiendo threads");
                Thread.currentThread().getThreadGroup().interrupt();
                return false;
            }
            this.PNet.trigger(t);
            if(this.verbose > 0){
                System.out.println("Transicion disparada: "+t.getName());
            }
            assertTrue(this.PNet.checkPInvariant());
            if((next_t = this.PNet.getNextTransition()) != null){
                this.ConditionQueue[next_t.getId()].signal();
            }
            return true;
        }catch(AssertionError e){
            System.out.println
                ("Msg From: "+Thread.currentThread().getName()+
                 "\n[!] ERROR: Algun invariante no se cumple, terminando");
            System.exit(-1);
            return false;
        }finally{
            if(this.lock.isHeldByCurrentThread())this.lock.unlock();
        }
    }
}
