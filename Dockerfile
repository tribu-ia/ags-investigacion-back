FROM python:3.12

WORKDIR /code

COPY requirements.txt /requirements.txt

ADD ./app /code/app

RUN pip install -r /requirements.txt

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--reload", "--host", "0.0.0.0", "--port", "8000"]
