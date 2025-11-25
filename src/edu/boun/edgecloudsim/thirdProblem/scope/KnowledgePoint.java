package edu.boun.edgecloudsim.thirdProblem.scope;

import java.util.List;

public class KnowledgePoint {
    private List<Offer> offerList;
    public KnowledgePoint(List<Offer> offers){
        offerList = offers;
    }
    public List<Offer> getOfferList() {
        return offerList;
    }

    public Offer getMaxUtility(){
        double max = Double.MIN_VALUE;
        Offer maxOffer = null;
        for(Offer offer : offerList){
            if(max < offer.getUtility()){
                max = offer.getUtility();
                maxOffer = offer;
            }
        }

        return maxOffer;
    }

    public Offer getMinUtility(){
        double min = Double.MAX_VALUE;
        Offer minOffer = null;
        for(Offer offer : offerList){
            if(min > offer.getUtility()){
                min = offer.getUtility();
                minOffer = offer;
            }
        }

        return minOffer;
    }
}
