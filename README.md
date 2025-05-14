# Umlego

Umlego is an extension of the open-source project MATSim. It integrates the SwissRailRapto module
within MATSim to compute demand assignment based on Origin-Destination (OD) matrices.

#### Table Of Contents

- [Introduction](#Introduction)
- [Getting Started](#Getting-Started)
- [Contributing](#Contributing)
- [Documentation](#Documentation)
- [Code of Conduct](#code-of-conduct)
- [Coding Standards](#coding-standards)
- [License](#License)

## Introduction

Umlego is software that allows for the computation of demand assignment. Given a public transport
timetable and time-dependent origin-destination matrices, it calculates the assigned demand for each
route found in the timetable.

The basic algorithm has been extended to allow for the computation of the Supply Effect. When
improving the service, it is expected to increase demand. This effect is calculated based on skim
matrices (Travel Time, Number of Changes, and Adjustment Time).

Each effect is governed by estimated elasticity based on count data. These values are not included
in this repository.

## Documentation

Links to all relevant documentation files, including:

- [CODING_STANDARDS.md](CODING_STANDARDS.md)
- [CONTRIBUTING.md](CONTRIBUTING.md)
- [LICENSE.md](LICENSE.md)

<a id="License"></a>

## License

This project is licensed under the GPL (General Public License) .

<a id="Contributing"></a>

## Contributing

Open-source projects thrive on collaboration and contributions from the community. To encourage
others to contribute to your project, you should provide clear guidelines on how to get involved.

This repository includes a [CONTRIBUTING.md](CONTRIBUTING.md) file that outlines how to contribute
to the project, including how to submit bug reports, feature requests, and pull requests.

<a id="coding-standards"></a>

## Coding Standards

To maintain a high level of code quality and consistency across your project, you should establish
coding standards that all contributors should follow.

This repository includes a [CODING_STANDARDS.md](CODING_STANDARDS.md) file that outlines the coding
standards that you should follow when contributing to the project.

<a id="code-of-conduct"></a>

## Code of Conduct

To ensure that your project is a welcoming and inclusive environment for all contributors, you
should establish a good [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)
