# AI Text Detector

This project implements an intelligent AI text detection UI.

## Architecture

The app is configured to use a self-managed detector backend instead of calling Gemini or Firebase directly from the browser.

- **API Service (`server.ts`)**: Handles health checks, metrics, and static asset serving.
- **Detector API Client (`src/services/aiDetectorApi.ts`)**: Sends text detection requests to `${API_URL}/api/ai-detector/detect`.
- **Local Persistence (`src/services/localDetections.ts`)**: Keeps recent UI history in browser storage only.
- **Observability Service (`src/components/dashboard`)**: Provides monitoring and analytics from local history.

## Detector Request

The frontend sends this payload shape:

```json
{
  "sourceType": "text",
  "link": null,
  "items": [
    {
      "id": 1,
      "text": "Text to analyze"
    }
  ]
}
```

## Configuration

Set `API_URL` in your environment. Leave it empty to use the same origin as the frontend.

```bash
API_URL="http://localhost:8080"
```

Firebase and database management are expected to live in your own backend.
