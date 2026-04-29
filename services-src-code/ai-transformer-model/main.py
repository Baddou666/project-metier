from fastapi import FastAPI
from transformers import pipeline
from pydantic import BaseModel

app = FastAPI()
classifier = pipeline("text-classification", model="./model")


class TextItem(BaseModel):
    id: int
    text: str

class InputPayload(BaseModel):
    items: list[TextItem]

class ItemPrediction(BaseModel):
    id: int
    label: str
    prediction: float


class OutputPayload(BaseModel):
    predictions: list[ItemPrediction]

@app.get("/")
async def root():
    return {"message": "Hello World"}

@app.post("/predict")
def predict(payload: list[TextItem]):
    results = []
    for item in payload:
        prediction = classifier(item.text)[0]
        output = {"id": item.id,
                  "prediction": prediction["score"],
                  "label": prediction["label"]
                  }
        results.append(output)
    return results

@app.get("/hello/{name}")
async def say_hello(name: str):
    return {"message": f"Hello {name}"}
