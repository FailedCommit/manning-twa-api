package com.twa.flights.api.itineraries.search.controller.documentation;

import java.util.List;

import org.springdoc.api.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import com.twa.flights.common.dto.itinerary.ItineraryDTO;
import com.twa.flights.common.dto.request.AvailabilityRequestDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Itineraries Search", description = "Operations about obtain itineraries with price")
public interface ItinerariesSearchResources {

    @Operation(description = "Get flights availability", responses = {
            @ApiResponse(responseCode = "200", description = "The itineraries with the conditions of search", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AvailabilityRequestDTO.class))) }, tags = {
                    "Itineraries Search" }, parameters = {
                            @Parameter(in = ParameterIn.QUERY, name = "from", description = "Every segment's origin comma separated. (e.g. BUE,MIA)", required = true, example = "BUE,MIA"),
                            @Parameter(in = ParameterIn.QUERY, name = "to", description = "Every segment's destination comma separated. (e.g. BUE,MIA)", required = true, example = "MIA,BUE"),
                            @Parameter(in = ParameterIn.QUERY, name = "departure", description = "Every segment's departure date comma separated. (e.g. 2021-10-29,2021-11-03)", required = true, example = "2021-10-29,2021-11-03"),
                            @Parameter(in = ParameterIn.QUERY, name = "adults", description = "Adults quantity. (e.g. 1)", required = true, example = "1"),
                            @Parameter(in = ParameterIn.QUERY, name = "children", description = "Children quantity. (e.g. 1))", required = true, example = "1"),
                            @Parameter(in = ParameterIn.QUERY, name = "infants", description = "Infants quantity. (e.g. 1)", required = true, example = "1"),
                            @Parameter(in = ParameterIn.QUERY, name = "amount", description = "The number of the itineraries to return", required = true, example = "10")

    })
    @GetMapping(value = "/itineraries")
    ResponseEntity<List<ItineraryDTO>> availability(@ParameterObject AvailabilityRequestDTO availabilityRequest);

}
