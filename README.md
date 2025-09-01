# Color Composer

## Description

Color Composer is a API server backend used for making light effects using WS2812/NeoPixel LED strips. It is written in Kotlin and uses the [Micronaut Framework](https://guides.micronaut.io/index.html) and uses Postgres as a database.

Color Composer does not interface with LEDs directly. Instead it sends color data to a [Pi Client](https://github.com/Cyborg-Squirrel/color-composer-client) or [NightDriver client](https://github.com/PlummersSoftwareLLC/NightDriverStrip/).

[Color Compser Web](https://github.com/Cyborg-Squirrel/color-composer-web) is the frontend for Color Composer.

## Requirements

*   JDK 21 or newer
*   A Postgres installation

## Installation

1. Configure your Postgres instance. Credentials can be passed as command line arguments or defined in the application.yaml file.
2. Build a Jar. This project uses the shadow plugin which packages all dependencies into a single Jar for ease of use. [Docker](https://guides.micronaut.io/latest/micronaut-docker-image-gradle-kotlin.html) images can also be built.
3. Run the Jar.

## Contributing

* Create a pull request
* Explain your changes
* Add unit tests
