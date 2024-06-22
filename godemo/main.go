package main

import (
	"fmt"
	"log"
	"net/http"

	"github.com/redis/go-redis/v9"
)

func main() {
	rdb := redis.NewClient(&redis.Options{
		Addr:           "100.100.100.100:6379",
		PoolSize:       200,
		MinIdleConns:   20,
		MaxActiveConns: 200,
		MaxIdleConns:   200,
	})

	http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		counter, err := rdb.Incr(r.Context(), "counter").Result()
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}
		fmt.Fprintf(w, "counter: %v", counter)
	})

	fmt.Println("vim-go")
	if err := http.ListenAndServe(":8080", nil); err != nil {
		log.Panic(err)
	}
}
