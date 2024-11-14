FROM grafana/grafana:latest

# Copy provisioning files into the container
COPY grafana/provisioning /etc/grafana/provisioning

# Ensure proper permissions
RUN chown -R 777 /etc/grafana/provisioning

