FROM python:3.11-slim

WORKDIR /workspace

ENV PYTHONDONTWRITEBYTECODE=1
RUN pip install --no-cache-dir \
    fastapi \
    uvicorn \
    transformers \
    torch

COPY . /workspace/

EXPOSE 8000

CMD ["python", "-m", "uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
