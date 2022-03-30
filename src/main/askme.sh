#!/usr/bin/env bash


while [[ $# -gt 0 ]]; do
  case $1 in
    -c|--core)
      core="$2"
      shift # past argument
      shift # past value
      ;;
    -l|--limit)
      limit="$2"
      shift # past argument
      shift # past value
      ;;
    -q|--queryfile)
      queryfile="$2"
      shift # past argument
      shift # past value
      ;;
    -h|--help)
      java -Xmx500m -jar service.jar -h
      exit;
      ;;
    -v|--version)
      java -Xmx500m -jar service.jar -v
      exit;
      ;;
    -*|--*)
      echo "Unknown option $1"
      exit 1
      ;;
  esac
done


java -Xmx500m -jar service.jar -c $core -l $limit -q $queryfile
