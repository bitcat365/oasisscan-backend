package common

type ValidatorGitInfo struct {
	Name        string `json:"name"`
	Path        string `json:"path"`
	Sha         string `json:"sha"`
	Size        int64  `json:"size"`
	Url         string `json:"url"`
	HtmlUrl     string `json:"html_url"`
	GitUrl      string `json:"git_url"`
	DownloadUrl string `json:"download_url"`
	Type        string `json:"type"`
	Links       Links  `json:"_links"`
}

type Links struct {
	Self string `json:"self"`
	Git  string `json:"git"`
	Html string `json:"html"`
}

type EntityInfo struct {
	Twitter string `json:"twitter"`
	Serial  int64  `json:"serial"`
	V       int64  `json:"v"`
	Name    string `json:"name"`
	Keybase string `json:"keybase"`
	Url     string `json:"url"`
	Email   string `json:"email"`
}
