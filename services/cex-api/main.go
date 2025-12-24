package main

import (
	"encoding/json"
	"log"
	"net/http"
	"os"

	"github.com/buke/quickjs-go"
)

type Request struct {
    ApiKey  string `json:"apiKey"`
    Timeout uint64 `json:"timeout"`
    Code    string `json:"code"`
}

type Response struct {
	Result string    `json:"result"`
	Error  ErrorCode `json:"error"`
}

type ErrorCode int

const (
	ErrorNone ErrorCode = iota
	ErrorTimeout
	ErrorExecution
)

func main() {
    apiKey := os.Getenv("CEX_API_KEY")

	http.HandleFunc("/api/execute", func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
			return
		}

		var req Request
		if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
			http.Error(w, "Bad request", http.StatusBadRequest)
			return
		}

		if req.ApiKey != apiKey {
			http.Error(w, "Invalid API Key", http.StatusUnauthorized)
			return
		}

		result, errorCode := executeCode(req.Code, req.Timeout)

		w.Header().Set("Content-Type", "application/json")
		err := json.NewEncoder(w).Encode(Response{Result: result, Error: errorCode})
		if err != nil {
			http.Error(w, "Internal Server Error", http.StatusInternalServerError)
			return
		}
	})

	port := os.Getenv("CEX_PORT")
	if port == "" {
		port = "8080"
	}

	log.Printf("Server starting on port %s", port)
	log.Fatal(http.ListenAndServe(":"+port, nil))
}

func executeCode(code string, timeout uint64) (string, ErrorCode) {
	rt := quickjs.NewRuntime()
	defer rt.Close()

	rt.SetExecuteTimeout(timeout)
	rt.SetMemoryLimit(128 * 1024 * 1024)

	ctx := rt.NewContext()
	defer ctx.Close()

	res := ctx.Eval(code)
	defer res.Free()

	if res.IsException() {
		err := ctx.Exception()

		switch {
		case err != nil && err.Error() == "InternalError: interrupted":
			return "", ErrorTimeout
        case err != nil:
            return err.Error(), ErrorExecution
		default:
			return "", ErrorExecution
		}
	}

	if res.IsNull() || res.IsUndefined() {
		return "null", ErrorNone
	}

	return res.ToString(), ErrorNone
}
