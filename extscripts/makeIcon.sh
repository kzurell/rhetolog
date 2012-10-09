#!/bin/bash


# Copyright (c) 2012 Kirk Zurell
#
# See the file LICENSE for copying permission.


BASE=./res

# makeIcon.sh "text to display" "filename.png"

# Debug lines
#   -pointsize 14 \
#    -stroke black \
#    -gravity South \
#    -draw "text 0,0 '$8'" \
    

_makeOneIcon () 
{
# size strokewidth lt rb radius textptsize labeltext size_density outname
convert -size $1 \
    -background transparent \
    -fill transparent \
    -strokewidth $2 \
    -stroke white \
    -draw "roundrectangle $3,$3 $4,$4 $5,$5" \
    -font Oswald-Regular \
    -pointsize $6 \
    -kerning 1 \
    -interline-spacing -5 \
    -strokewidth 0 \
    -stroke white \
    -fill white \
    -gravity center \
    label:"$7" \
    $BASE/drawable-$8/$9

}


# 3,4,6,8

# xlarge : 75 100 150 200
# large : 60 80 120 160


mkdir -p $BASE/drawable-large-ldpi
_makeOneIcon 60x60 3 2 58 6 16 "$1" large-ldpi $2
mkdir -p $BASE/drawable-large-mdpi
_makeOneIcon 80x80 3 2 78 6 22 "$1" large-mdpi $2
mkdir -p $BASE/drawable-large-hdpi
_makeOneIcon 120x120 3 2 118 6 36 "$1" large-hdpi $2
mkdir -p $BASE/drawable-large-xhdpi
_makeOneIcon 160x160 3 2 158 6 57 "$1" large-xhdpi $2

# xlarge screen
mkdir -p $BASE/drawable-xlarge-ldpi
_makeOneIcon 75x75 3 3 72 10 21 "$1" xlarge-ldpi $2
mkdir -p $BASE/drawable-xlarge-mdpi
_makeOneIcon 100x100 3 3 97 10 33 "$1" xlarge-mdpi $2
mkdir -p $BASE/drawable-xlarge-hdpi
_makeOneIcon 150x150 3 3 147 10 52 "$1" xlarge-hdpi $2
mkdir -p $BASE/drawable-xlarge-xhdpi
_makeOneIcon 200x200 3 3 197 10 70 "$1" xlarge-xhdpi $2




