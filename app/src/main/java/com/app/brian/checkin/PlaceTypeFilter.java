package com.app.brian.checkin;

import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by zhanghan177
 * Used by Brian 10/30/2017
 */

public class PlaceTypeFilter {

    private Set<Integer> mAllowedTypes;
    private Set<Integer> mDisallowedTypes;

    public PlaceTypeFilter(int allowedTypes[], int disallowedTypes[]) {
        mAllowedTypes = new HashSet<>();
        for (int type : allowedTypes) {
            mAllowedTypes.add(type);
        }
        mDisallowedTypes = new HashSet<>();
        for (int type : disallowedTypes) {
            mDisallowedTypes.add(type);
        }
    }

    private boolean hasMatchingType(Place place) {
        List<Integer> types = place.getPlaceTypes();
        for (int type : types) {
            if (mDisallowedTypes.contains(type)) {
                return false;
            }
        }
        for (int type : types) {
            if (mAllowedTypes.contains(type)) {
                return true;
            }
        }
        return false;
    }

    public List<PlaceLikelihood> filteredPlaces(PlaceLikelihoodBuffer places) {
        List<PlaceLikelihood> results = new ArrayList<>();
        for (PlaceLikelihood likelihood : places) {
            if (hasMatchingType(likelihood.getPlace())) {
                results.add(likelihood);
            }
        }
        return results;
    }

}
