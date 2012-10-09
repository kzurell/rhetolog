#!/usr/bin/awk -f

BEGIN {
    RS="YYY"; 
    FS=":";
}

{ 
    if($1 != "")
    { 
	$3 = "\"" $3 "\""; 
	TORUN = "./makeIcon.sh " $1 ".png " $3;
	print TORUN; 
	system(TORUN);
    }
}
