package utils

import (
	"io"
	"log/slog"
)

// Log is a no-op logger that discards all output
var Log *slog.Logger

func init() {
	// Initialize with a discard handler (no logging)
	Log = slog.New(slog.NewTextHandler(io.Discard, nil))
}
