export SPECIES=Adetomyrma_MG01
export DATA_DIR=/var/geotrellis/eco/ecodata/

for dir in $(ls -1 -d ${DATA_DIR}/*/ ) ; do
  
  export SPECIES=`basename $dir`
  echo "dir: $dir"
  echo "species: $SPECIES"
  sleep 1
  cd $dir

  for subdir in $(ls -1 -d $dir/*/); do
    cd $subdir 
    gdal_translate -ot Byte -of ARG -scale 0.0 1.0 0 100 -a_srs EPSG:4326 $SPECIES.asc $SPECIES.arg
    sed -i 's/uint8/int8/' *.json
    export THRESHOLD=`perl -n -e 'if ($_ =~ /(\d.\d\d\d)..t.\><td\>Equal training sensitivity and specificity/) { print $1 }' ${SPECIES}.html`
    echo $THRESHOLD > $SPECIES_threshold.txt
  done
done
