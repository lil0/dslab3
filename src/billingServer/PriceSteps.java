
package billingServer;

import billingServer.PriceSteps.PriceStep;
import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;

/**
 *
 * @author lisibauernhofer
 */
public class PriceSteps implements Serializable{
    private static final long serialVersionUID = -2191451042278513296L;

    private ArrayList<PriceStep> priceSteps;
    private boolean deleteStep = false;
    public PriceSteps() {
        priceSteps = new ArrayList<PriceStep>();
    }

    public void addPriceStep(double startPrice, double endPrice, double fixedPrice, double variablePricePercent) throws RemoteException {
        if(startPrice<0 || endPrice<0 || fixedPrice<0 || variablePricePercent<0){
            throw new RemoteException("Negative values are not allowed");
        }
        if(endPrice != 0)
        {
            if(startPrice>endPrice){
                throw new IllegalArgumentException("The min price is higher than the max price");
            }
        }

            PriceStep newPS = new PriceStep(startPrice, endPrice,fixedPrice,variablePricePercent);
            
            synchronized(priceSteps){
                for (PriceStep helpPS : priceSteps){
                    if(newPS.collide(helpPS)== true){
                        throw new NumberFormatException("The price interval collides with an existing price step");
                    }                    
                }
             this.priceSteps.add(newPS);

            }               
    }

    void deleteStep(double startPrice, double endPrice) throws RemoteException {
        deleteStep = false;

        synchronized(priceSteps){
            for (int i = 0; i< priceSteps.size(); i++){
                if(priceSteps.get(i).minPrice == startPrice && priceSteps.get(i).maxPrice == endPrice){
                    priceSteps.remove(i);
                    deleteStep = true;
                }
            }
        }
        if(deleteStep == false){
            throw new IllegalArgumentException("The price step does not exist");
        }
    }

     

     public double getFeeFixByPrice(double price){
         
         synchronized(priceSteps){
             for(PriceStep helpPS: priceSteps){
                if(price > helpPS.getMinPrice() && helpPS.getMaxPrice()==0 || price <= helpPS.getMaxPrice()){
                    return helpPS.getFeeFixed();
                }
            }  return 0;
         }
     }

     public double getFeeVariableByPrice(double price){
        synchronized(priceSteps){
            for(PriceStep helpPS: priceSteps){
                if(price > helpPS.getMinPrice()&& helpPS.getMaxPrice()==0 || price <= helpPS.getMaxPrice()){
                    return helpPS.getFeeVariable();
                }
            }  return 0;
        }
     }



    
    @Override
    public String toString(){
        String liste = "";
        String header = "Min_Price\tMax_Price\tFee_Fixed\tFee_variable";
        synchronized(priceSteps){

            for(PriceStep pricestep : priceSteps){

                liste += pricestep.formatPriceStep() +"\n";

            }
            return header + "\n"+ liste;
        }
    }





    public class PriceStep implements Serializable {


        private double minPrice;
        private double maxPrice;
        private double feeFixed;
        private double feeVariable;

        PriceStep(double minPrice, double maxPrice, double feeFixed, double feeVariable){
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.feeFixed = feeFixed;
        this.feeVariable = feeVariable;
        }

        PriceStep(){
            
        }

        public String formatPriceStep() {
        //%.2f -> Fliesskommerzahl die auf 2 Stellen gerundet wird
        //\t -> Tab
        //%% Prozentzeichen
            if(maxPrice == 0){
                return String.format(" %.2f\t    INFINITY\t %.1f\t     %.1f%%",minPrice, feeFixed, feeVariable);
            } else {
                return String.format(" %.2f\t    %.2f\t    %.1f\t     %.1f%%",minPrice, maxPrice, feeFixed, feeVariable);

            }
        }

        public boolean collide(PriceStep step){

            if((this.maxPrice != 0 && step.maxPrice!= 0)){
                if(this.minPrice>=step.maxPrice){
                    return false;
                }
                if(this.maxPrice<=step.minPrice){
                    return false;
                }
                
                return true;
            }
            if(step.maxPrice == 0 && step.minPrice<= this.maxPrice){
                    return true;
                }

            if(this.maxPrice == 0 && step.maxPrice == 0) return true;
            else
                return false;
        }



        public boolean maxMin(PriceStep step){
            if(this.minPrice > this.maxPrice){
                return false;
            }else return true;
        }
        
       




    public double getFeeFixed(){
        return this.feeFixed;
    }

    public double getFeeVariable(){
        return this.feeVariable;
    }

    public double getMaxPrice(){
        return this.maxPrice;
    }

    public double getMinPrice(){
        return this.minPrice;
    }

        
    }


}
