package main

import (
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"sync"

	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

type Order struct {
	ID       string       `json:"id"`
	Items    []ItemInfo   `json:"items"`
	Customer CustomerInfo `json:"customer"`
}

type ItemInfo struct {
	ID     string `json:"id"`
	Item   Item   `json:"item"`
	Cost   int    `json:"cost"`
	Amount int    `json:"amount"`
}

type Item struct {
	Name string `json:"name"`
}

type CustomerInfo struct {
	ID   string `json:"id"`
	Name string `json:"name"`
	City string `json:"city"`
}

var (
	orderMap = make(map[string]Order)
	mu       sync.Mutex
)

var (
	requestDuration = prometheus.NewHistogramVec(
		prometheus.HistogramOpts{
			Name:    "http_request_duration_seconds",
			Help:    "Duration of HTTP requests",
			Buckets: prometheus.DefBuckets,
		}, []string{"app", "endpoint"},
	)
)

func main() {
	port := os.Getenv("APP_PORT")

	if port == "" {
		log.Println("port isn't specified")
		port = "8080"
	}

	prometheus.MustRegister(requestDuration)

	http.HandleFunc("/order/create", createOrder)
	http.HandleFunc("/order", getOrder)

	http.Handle("/metrics", promhttp.Handler())

	fmt.Printf("Server running on http://localhost:%v\n", port)
	err := http.ListenAndServe(fmt.Sprintf(":%v", port), nil)
	if err != nil {
		fmt.Printf("Server failed to start: %v\n", err)
		os.Exit(1)
	}

}

func createOrder(rw http.ResponseWriter, r *http.Request) {
	timer := prometheus.NewTimer(requestDuration.WithLabelValues("go", "/order/create"))
	defer timer.ObserveDuration()

	if r.Header.Get("Content-Type") != "application/json" {
		http.Error(rw, "Expected 'Content-Type: application/json' header", http.StatusUnsupportedMediaType)
		return
	}

	body, err := io.ReadAll(r.Body)
	if err != nil {
		http.Error(rw, "Failed to read request body", http.StatusBadRequest)
		return
	}

	var order Order
	err = json.Unmarshal(body, &order)
	if err != nil {
		http.Error(rw, "Can't parse JSON", http.StatusBadRequest)
		return
	}

	mu.Lock()
	defer mu.Unlock()
	if _, exists := orderMap[order.ID]; exists {
		http.Error(rw, fmt.Sprintf("Order for %s already exists", order.ID), http.StatusConflict)
		return
	}

	orderMap[order.ID] = order
	rw.WriteHeader(http.StatusCreated)
	rw.Write([]byte(fmt.Sprintf("Order created with ID: %s", order.ID)))
}

func getOrder(rw http.ResponseWriter, r *http.Request) {
	timer := prometheus.NewTimer(requestDuration.WithLabelValues("go", "/order"))
	defer timer.ObserveDuration()

	id := r.URL.Query().Get("id")

	if id == "" {
		http.Error(rw, "Missing order ID in path", http.StatusBadRequest)
		return
	}

	mu.Lock()
	order, exists := orderMap[id]
	mu.Unlock()

	if exists {
		orderJSON, err := json.Marshal(order)
		if err != nil {
			http.Error(rw, "Failed to serialize order", http.StatusInternalServerError)
			return
		}

		rw.Header().Set("Content-Type", "application/json")
		rw.WriteHeader(http.StatusOK)
		rw.Write(orderJSON)
	} else {
		http.Error(rw, fmt.Sprintf("Order for %s does not exist", id), http.StatusNotFound)
	}
}
