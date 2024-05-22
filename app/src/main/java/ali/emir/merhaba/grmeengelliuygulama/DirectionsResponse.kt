package ali.emir.merhaba.grmeengelliuygulama

data class DirectionsResponse(
    val routes: List<Route>
)

data class Route(
    val overview_polyline: OverviewPolyline,
    val legs: List<Leg> // legs özelliği List<Leg> türünde tanımlanıyor
)


data class OverviewPolyline(
    val points: String
)

data class Leg(
    val steps: List<Step>
)

data class Step(
    val start_location: Location,
    val end_location: Location,
    val html_instructions: String,
    val polyline: Polyline,
    val distance: Distance

)

data class Distance(
    val text: String,
    val value: Int
)


data class Location(
    val lat: Double,
    val lng: Double
)
data class Polyline(
    val points: String
)
