#!/usr/bin/env bash
docker buildx build --platform=linux/amd64 -t dejankovacevic/soyka:latest .
docker push dejankovacevic/soyka
kubectl delete pod -l app=soyka -n staging
kubectl logs -n staging -l app=soyka -f