# Use Python 3.9 as the base image
FROM python:3.9-slim

# Set the working directory
WORKDIR /app

# Copy requirements file
COPY requirements.txt .

# Install dependencies with increased timeout for large packages
RUN pip install --no-cache-dir --default-timeout=1000 -r requirements.txt

# Preload EasyOCR models to avoid delays during first use
# Create directory for models
RUN mkdir -p /root/.EasyOCR/model
# Pre-download and cache the models
RUN python -c "import easyocr; reader = easyocr.Reader(['en'], download_enabled=True); print('EasyOCR models preloaded successfully')"

# Copy the application code
COPY . .

# Expose the port the app runs on
EXPOSE 8084

# Run the application
CMD ["python", "main.py"]