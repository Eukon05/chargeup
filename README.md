# ChargeUP API

An API for monitoring the energy mix in Great Britain for the optimal time to charge an EV with clean energy.

## Live demo

The API is available at: https://chargeup-backend.onrender.com  
Documentation and interactive playground are available
at: https://chargeup-backend.onrender.com/api/v1/swagger-ui/index.html

## Functionality

ChargeUP is an EV charging optimization app.
Based on data about the energy mix in Great Britain, it can calculate an optimal window of time in the next two days,
when the energy mix there has the highest share of clean energy sources available.
The user can specify how long their charging window is, and the app will calculate an optimal window of that length.

The API exposes two endpoints:

- `/api/v1/energy/current` - returns the average daily energy mix for today and predicted for two days into the future
- `/api/v1/energy/window?windowLength=X` - returns the optimal time window for charging an EV based on the energy mix
  data and the specified charging duration in hours.

Example response from `/api/v1/energy/current`:

```json
{
  "mixes": [
    {
      "date": "2026-06-18",
      "sourceMix": {
        "hydro": 0.8645833333333334,
        "other": 1.2874999999999999,
        "biomass": 5.947916666666667,
        "imports": 14.358333333333334,
        "gas": 31.287499999999998,
        "solar": 10.674999999999999,
        "coal": 0.0,
        "nuclear": 9.75625,
        "wind": 25.797916666666666
      },
      "cleanPerc": 53.041666666666664
    },
    {
      "date": "2026-06-19",
      "sourceMix": {
        "hydro": 0.0,
        "other": 0.0,
        "biomass": 6.677083333333333,
        "imports": 12.633333333333333,
        "gas": 22.952083333333334,
        "solar": 13.204166666666666,
        "coal": 0.0,
        "nuclear": 9.104166666666666,
        "wind": 35.41041666666667
      },
      "cleanPerc": 64.39583333333333
    },
    {
      "date": "2026-06-20",
      "sourceMix": {
        "hydro": 0.0,
        "other": 0.0,
        "biomass": 7.704878048780487,
        "imports": 18.282926829268295,
        "gas": 21.985365853658536,
        "solar": 16.75609756097561,
        "coal": 0.0,
        "nuclear": 9.648780487804878,
        "wind": 25.602439024390247
      },
      "cleanPerc": 59.71219512195122
    }
  ]
}
```

Example response from `/api/v1/energy/window?windowLength=6`:

```json
{
  "from": "2026-06-19T08:30:00",
  "to": "2026-06-19T14:30:00",
  "cleanPerc": 77.30833333333335
}
```

Window length has to be a number in range starting at 1 and ending at 6.  
The API handles an invalid window length by returning a 400 error with a message indicating the valid range.

Example response for an invalid window length:

```json
{
  "getOptimalWindow.windowLength": "musi należeć do zakresu od 1 do 6"
}
```

The data is sourced from https://carbon-intensity.github.io/api-definitions/?shell#get-generation-from-to  
When the external API is down, the ChargeUP API will return a 503 error with a message indicating that the external API
is unavailable.

Example: `An exception occurred while trying to fetch from the API`

The complete API documentation, along with an interactive playground is available at
`[serveraddr]/api/v1/swagger-ui/index.html`

## Tech stack

The project was build using the following technologies:

- Java 21
- Spring Boot
- Maven
- JUnit 5
- Mockito
- OpenAPI/Swagger
- Lombok
- Docker
- GitHub Actions

## Testing

The API is covered by unit and integration tests.  
Service classes are being tested by mocking the external API client, while controller tests use MockMvc to test the
endpoints.  
We check both for positive and negative scenarios, such as when a user provides invalid input.  
Tests are automatically executed by GitHub Actions on every push to the master branch of this repository.

## Development setup

- Make sure to have JDK 21 installed on your machine. You can download it
  from [Adoptium](https://adoptium.net/temurin/releases/?version=21).
- Open a terminal window inside a directory you wish to clone the repository to.
- Clone the repository to your machine, using git clone `https://github.com/Eukon05/chargeup-frontend.git .`
- In the project root, run `./mvnw spring-boot:run` to start the application.`
- The API will be available at `http://localhost:8080` by default.

## Production setup

For production deployments, it is recommended to use a Docker image provided in this repo.

- Make sure to have Docker installed on your machine. You can download it
  from [here](https://www.docker.com/get-started).
- Clone the repository to your machine the same way as for development setup.
- In the project root, run `docker build -t chargeup-api .` to build the Docker image.
- After the image is built, run `docker run -p 8080:8080 chargeup-api` to start the application.
- The API will be available at `http://localhost:8080` by default. You can modify the port mapping by changing the
  command above, if you'd like to use a different port.