const fs = require('fs');
let code = fs.readFileSync('D:/Soft/safe-travel-web/components/MapboxMonitor.tsx', 'utf8');

const missingCode = `
  // 3.4 Geocode custom addresses
  useEffect(() => {
    if (!isTokenConfigured || !mapboxToken) return;
    let active = true;

    const geocodeAllNeeded = async () => {
      let changed = false;
      const newResolved = {};

      const locationsToGeocode = new Set<string>();
      activeTripsList.forEach(trip => {
        if (trip.start_location && !trip.start_coords && !resolvedCoords[trip.start_location] && !getStaticLocationCoords(trip.start_location)) {
          locationsToGeocode.add(trip.start_location);
        }
        if (trip.end_location && !trip.end_coords && !resolvedCoords[trip.end_location] && !getStaticLocationCoords(trip.end_location) && !trip.end_location.includes('Unknown Destination')) {
          locationsToGeocode.add(trip.end_location);
        }
      });
      
      if (newTripStart && !resolvedCoords[newTripStart] && !getStaticLocationCoords(newTripStart)) {
        locationsToGeocode.add(newTripStart);
      }

      for (const loc of locationsToGeocode) {
        try {
          const url = \`https://api.mapbox.com/geocoding/v5/mapbox.places/\${encodeURIComponent(loc)}.json?access_token=\${mapboxToken}&limit=1\`;
          const res = await fetch(url);
          const data = await res.json();
          if (data.features && data.features.length > 0) {
            newResolved[loc] = data.features[0].center as [number, number];
            changed = true;
          }
        } catch (err) {
          console.warn('Geocoding error for', loc, err);
        }
      }

      if (active && changed) {
        setResolvedCoords(prev => ({ ...prev, ...newResolved }));
      }
    };

    geocodeAllNeeded();
    return () => { active = false; };
  }, [activeTripsList, isTokenConfigured, mapboxToken, newTripStart]);

  // Background geocoding and routing
  useEffect(() => {
    if (!isTokenConfigured || !mapboxToken || activeTripsList.length === 0) return;

    let active = true;

    const fetchNeededRoutes = async () => {
      let changed = false;
      const newGeometries = { ...roadGeometries };

      for (const trip of activeTripsList) {
        if (extractRouteLog(trip.route_path_log)) continue; // Already has DB log

        const startCoords = (trip.start_coords || getCoordsSync(trip.start_location, resolvedCoords));
        const endCoords = (trip.end_coords || getCoordsSync(trip.end_location, resolvedCoords));
        const isUnknownDest = !trip.end_location || trip.end_location.includes('Unknown Destination');
        if (isUnknownDest) continue;

        const isStartFallback = startCoords[0] === 90.4125 && startCoords[1] === 23.8103;
        const isEndFallback = endCoords[0] === 90.4125 && endCoords[1] === 23.8103;
        if (isStartFallback || isEndFallback) continue;

        const key = \`\${trip.id}-\${trip.start_location}-\${endCoords[0].toFixed(4)},\${endCoords[1].toFixed(4)}\`;
        if (roadGeometries[key] || newGeometries[key]) continue;

        try {
          const url = \`https://api.mapbox.com/directions/v5/mapbox/driving-traffic/\${startCoords[0]},\${startCoords[1]};\${endCoords[0]},\${endCoords[1]}?geometries=geojson&overview=full&access_token=\${mapboxToken}\`;
          const res = await fetch(url);
          const data = await res.json();
          if (data.routes && data.routes[0]?.geometry?.coordinates) {
            newGeometries[key] = data.routes[0].geometry.coordinates;
            changed = true;
          }
        } catch (err) {
          console.error("Mapbox Directions API error:", err);
        }
      }

      if (active && changed) {
        setRoadGeometries(prev => ({ ...prev, ...newGeometries }));
      }
    };

    fetchNeededRoutes();

    return () => { active = false; };
  }, [activeTripsList, isTokenConfigured, mapboxToken, resolvedCoords]);

  // 3.6. Manage route lines/paths on Mapbox GL Map
  useEffect(() => {
    if (!mapRef.current || !isTokenConfigured || !mapboxToken) return;

    const mapInstance = mapRef.current;
    
    const updateRoutes = () => {
      activeTripsList.forEach((trip) => {
        const isSelected = trip.id === selectedTripId;
        const sourceId = \`route-source-\${trip.id}\`;
        const layerId = \`route-layer-\${trip.id}\`;
        const shadowLayerId = \`route-shadow-\${trip.id}\`;

        const endCoords = (trip.end_coords || getCoordsSync(trip.end_location, resolvedCoords));
        const key = \`\${trip.id}-\${trip.start_location}-\${endCoords[0].toFixed(4)},\${endCoords[1].toFixed(4)}\`;
        
        const rawCoords = extractRouteLog(trip.route_path_log) || roadGeometries[key] || generateSimulatedRoadRoute(
          (trip.start_coords || getCoordsSync(trip.start_location, resolvedCoords)), 
          endCoords
        );

        if (!mapInstance.getSource(sourceId)) {
          mapInstance.addSource(sourceId, {
            type: 'geojson',
            data: {
              type: 'Feature',
              properties: {},
              geometry: {
                type: 'LineString',
                coordinates: rawCoords
              }
            }
          });

          mapInstance.addLayer({
            id: shadowLayerId,
            type: 'line',
            source: sourceId,
            layout: { 'line-join': 'round', 'line-cap': 'round' },
            paint: {
              'line-color': '#0f172a',
              'line-width': isSelected ? 10 : 6,
              'line-opacity': 0.6,
              'line-blur': 4
            }
          });

          mapInstance.addLayer({
            id: layerId,
            type: 'line',
            source: sourceId,
            layout: { 'line-join': 'round', 'line-cap': 'round' },
            paint: {
              'line-color': isSelected ? '#3b82f6' : '#14b8a6',
              'line-width': isSelected ? 6 : 3,
              'line-opacity': isSelected ? 1 : 0.6
            }
          });
        } else {
          const sourceObj = mapInstance.getSource(sourceId);
          if (sourceObj) {
            sourceObj.setData({
              type: 'Feature',
              properties: {},
              geometry: {
                type: 'LineString',
                coordinates: rawCoords
              }
            });
          }
          if (mapInstance.getLayer(layerId)) {
            mapInstance.setPaintProperty(layerId, 'line-color', isSelected ? '#3b82f6' : '#14b8a6');
            mapInstance.setPaintProperty(layerId, 'line-width', isSelected ? 6 : 3);
            mapInstance.setPaintProperty(layerId, 'line-opacity', isSelected ? 1 : 0.6);
          }
          if (mapInstance.getLayer(shadowLayerId)) {
            mapInstance.setPaintProperty(shadowLayerId, 'line-width', isSelected ? 10 : 6);
          }
        }
      });
    };

    updateRoutes();

  }, [activeTripsList, selectedTripId, isTokenConfigured, mapboxToken, resolvedCoords, roadGeometries, mapLoadedTrigger]);

  // 3.5. Render Interactive Destination Pin Coordinates (with dynamic road-snapping and self-cleanup)
`;

code = code.replace(/\/\/\s*3\.5\.\s*Render Interactive Destination Pin Coordinates \(with dynamic road-snapping and self-cleanup\)/, missingCode);

fs.writeFileSync('D:/Soft/safe-travel-web/components/MapboxMonitor.tsx', code);
