package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"os"
	"strconv"
	"time"

	"github.com/google/uuid"
)

type Order struct {
	ID       string       `json:"id"`
	Items    []ItemInfo   `json:"items"`
	Customer CustomerInfo `json:"customer"`
}

// ItemInfo represents the items in the order
type ItemInfo struct {
	ID     string `json:"id"`
	Item   Item   `json:"item"`
	Cost   int    `json:"cost"`
	Amount int    `json:"amount"`
}

// Item represents a single item with a name
type Item struct {
	Name string `json:"name"`
}

// CustomerInfo represents customer details
type CustomerInfo struct {
	ID   string `json:"id"`
	Name string `json:"name"`
	City string `json:"city"`
}

func main() {
	var (
		host string
		port string
		rps  int
	)

	host = os.Getenv("TARGET_APP_HOST")
	port = os.Getenv("TARGET_APP_PORT")
	rps, err := strconv.Atoi(os.Getenv("TARGET_LOAD_RPS"))

	if err != nil {
		log.Fatal("can't parse rps")
	}

	client := &http.Client{}
	ticker := time.NewTicker(time.Second / time.Duration(rps))

	createOrderUrl := fmt.Sprintf("http://%s:%s/order/create", host, port)
	getOrderUrl := func(id string) string {
		return fmt.Sprintf("http://%s:%s/order?id=%s", host, port, id)
	}

	for range ticker.C {
		go func() {
			id := uuid.New().String()
			order := Order{
				ID: id,
				Items: []ItemInfo{
					{
						ID: id,
						Item: Item{
							Name: "Laptop",
						},
						Cost:   1000,
						Amount: 2,
					},
					{
						ID: id,
						Item: Item{
							Name: "Mouse",
						},
						Cost:   50,
						Amount: 1,
					},
				},
				Customer: CustomerInfo{
					ID:   id,
					Name: "John Doe",
					City: "New York",
				},
			}

			jsonData, err := json.Marshal(order)

			if err != nil {
				log.Fatal("can't serialize order")
			}

			resp1, err := client.Post(createOrderUrl, "application/json", bytes.NewBuffer(jsonData))

			if err != nil {
				// log.Printf("error sending create order request: %v", err)
				return
			}

			resp2, err := client.Get(getOrderUrl(id))
			if err != nil {
				// log.Printf("error sending request: %v", err)
				return
			}

			defer resp1.Body.Close()
			defer resp2.Body.Close()
		}()
	}
}
