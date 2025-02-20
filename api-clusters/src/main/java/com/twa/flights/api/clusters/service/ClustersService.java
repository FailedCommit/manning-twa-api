package com.twa.flights.api.clusters.service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.twa.flights.api.clusters.helper.FlightIdGeneratorHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.twa.flights.api.clusters.dto.ClusterSearchDTO;
import com.twa.flights.api.clusters.dto.request.ClustersAvailabilityRequestDTO;
import com.twa.flights.api.clusters.enums.ExceptionStatus;
import com.twa.flights.api.clusters.exception.APIException;
import com.twa.flights.api.clusters.repository.ClustersRepository;
import com.twa.flights.common.dto.itinerary.ItineraryDTO;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@Service
public class ClustersService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClustersService.class);

    private final ItinerariesSearchService itinerariesSearchService;
    private final PricingService pricingService;
    private final ClustersRepository repository;
    private final FlightIdGeneratorHelper flightIdGeneratorHelper;

    @Autowired
    public ClustersService(ItinerariesSearchService itinerariesSearchService, PricingService pricingService,
            ClustersRepository repository, FlightIdGeneratorHelper flightIdGeneratorHelper) {
        this.itinerariesSearchService = itinerariesSearchService;
        this.pricingService = pricingService;
        this.repository = repository;
        this.flightIdGeneratorHelper = flightIdGeneratorHelper;
    }

    public ClusterSearchDTO availability(ClustersAvailabilityRequestDTO request) {
        LOGGER.debug("begin the search");
        ClusterSearchDTO response = getPreviousSearchResult(request);
        if (isNull(response) && isEmpty(request.getId()))
            response = availabilityFromProviders(request);
        else
            response = availabilityFromDatabase(request);

        return response;
    }

    private ClusterSearchDTO getPreviousSearchResult(ClustersAvailabilityRequestDTO request) {
        ClusterSearchDTO response = repository.get(flightIdGeneratorHelper.generate(request));
        if (nonNull(response)) {
            LOGGER.info("\n\n\n*************** Response: {} ***************\n\n\n", response);
            final List<ItineraryDTO> itineraries = response.getItineraries();
            response.setItineraries(itineraries.stream().limit(request.getAmount()).collect(Collectors.toList()));
        }
        return response;
    }

    private ClusterSearchDTO availabilityFromProviders(ClustersAvailabilityRequestDTO request) {
        ClusterSearchDTO response;
        List<ItineraryDTO> itineraries = itinerariesSearchService.availability(request);

        itineraries = pricingService.priceItineraries(itineraries);
        itineraries = itineraries.stream().sorted((itineraryOne, itineraryTwo) -> itineraryOne.getPriceInfo()
                .getTotalAmount().compareTo(itineraryTwo.getPriceInfo().getTotalAmount())).collect(Collectors.toList());

        response = repository.insert(request, itineraries);

        // Limit the size
        response.setItineraries(itineraries.stream().limit(request.getAmount()).collect(Collectors.toList()));
        return response;
    }

    private ClusterSearchDTO availabilityFromDatabase(ClustersAvailabilityRequestDTO request) {
        ClusterSearchDTO response;
        response = repository.get(request.getId());

        if (response == null) {
            throw new APIException(HttpStatus.BAD_GATEWAY, ExceptionStatus.SEARCH_NOT_FOUND_IN_REPOSITORY.getCode(),
                    ExceptionStatus.SEARCH_NOT_FOUND_IN_REPOSITORY.getMessage());
        }

        response.getPagination().setOffset(request.getOffset()); // Update offset

        List<ItineraryDTO> itineraries = response.getItineraries();

        long skip = request.getOffset().longValue() * request.getAmount();

        // Limit the size
        response.setItineraries(
                itineraries.stream().skip(skip).limit(request.getAmount()).collect(Collectors.toList()));
        return response;
    }

}
