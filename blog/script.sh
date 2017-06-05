#!/usr/bin/env bash

$1 --title 'California' --xlabel 'Day' --ylabel 'Temperature (Kelvin)' --infiles place.csv min.csv mean.csv max.csv --mu '#000000' '#0000ff' '#00ff00' '#ff0000' --names "\$33^{\\circ}53'N\$ \$118^{\\circ}13'W\$" min '$\mu$' max --outfile plot.pdf
