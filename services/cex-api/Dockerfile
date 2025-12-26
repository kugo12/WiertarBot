FROM golang:1.25-alpine AS builder

WORKDIR /app

RUN apk add --no-cache gcc musl-dev

COPY go.mod ./

RUN go mod download

COPY . .

RUN go build -o cex-api .

FROM alpine:latest

WORKDIR /app

COPY --from=builder /app/cex-api .

EXPOSE 8080

CMD ["./cex-api"]
