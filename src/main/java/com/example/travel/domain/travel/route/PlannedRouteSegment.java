package com.example.travel.domain.travel.route;

public record PlannedRouteSegment(int dayNumber, int segmentOrder,
                                  RoutePoint from, RoutePoint to,
                                  RouteSegmentResult route) {
}
