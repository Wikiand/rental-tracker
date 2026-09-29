#!/bin/sh
set -eu

if [ ! -f /app/data/rental-tracker.db ]; then
    echo "Initializing persistent database from seeded database..."
    cp /app/seed/rental-tracker.db /app/data/rental-tracker.db
else
    echo "Using existing persistent database..."
fi

exec "$@"
