(assembly-load-with-partial-name "System.Windows.Forms")

(ns core.fs
  (:import
   (System.IO
    Directory
    DirectoryInfo
    File
    FileStream
    FileMode
    Path)
   (System.Threading
    ApartmentState
    Thread
    ThreadStart)
   (System.Windows.Forms
    Form
    FolderBrowserDialog
    SaveFileDialog
    DialogResult)))

(defn folder-browser-dialog [wrapper]
  (try
    (let [dialog (FolderBrowserDialog.)]
      (if (= (.ShowDialog dialog (.FindForm (:browser wrapper))) DialogResult/OK)
        {:path (.SelectedPath dialog)}
        {:cancel true}))
    (catch Exception e {:exception (.get_Message e)})))

(defn last-write-time [path _]
  (try
    (if (File/Exists path)
      (File/GetLastWriteTimeUtc path)
      nil)
    (catch Exception e
      nil)))

(defn read-all-text [path _]
  (try
    {:name (Path/GetFileName path)
     :text (File/ReadAllText path)
     :last-write-time (File/GetLastWriteTimeUtc path)}
    (catch Exception e {:exception (.get_Message e)})))

(defn combine-folder-path [m _]
  (let [folder-path (:folder-path m)
        file-name (:file-name m)]
    (if (and folder-path file-name)
      (try
        (assoc m :path (Path/Combine folder-path file-name))
        (catch Exception e (assoc m :exception (.get_Message e))))
      m)))

(defn create [m _]
  (let [path (:path m)]
    (if path
      (try
        (with-open [fs (FileStream. path FileMode/CreateNew)]
          (.Flush fs)
          m)
        (catch Exception e
          (-> m
              (dissoc :path)
              (assoc :exception (.get_Message e)))))
      m)))

(defn delete [path _]
  (try
    (File/Delete path)
    nil
    (catch Exception e {:exception (.get_Message e)})))

(defn save
  "Returns {:status #{:success :exception} :result {:name string :last-write-time date-time} string}"
  [path text _]
  (try
    (File/WriteAllText path text)
    {:status :success
     :result {:name (Path/GetFileName path)
              :last-write-time (File/GetLastWriteTimeUtc path)}}
    (catch Exception e {:status :exception :result (.get_Message e)})))

(defn- save-file-dialog [initial-directory initial-file-name]
  (doto (SaveFileDialog.)
    (.set_FileName initial-file-name)
    (.set_Filter "Clojure file (*.clj)|*.clj")
    (.set_FilterIndex 0)
    (.set_InitialDirectory initial-directory)
    (.set_ValidateNames true)
    (.set_OverwritePrompt true)
    (.set_AddExtension true)))

(defn save-as
  "Returns {:status #{:success :cancel :exception} :result {:path string} string}"
  [initial-directory initial-file-name _]
  (try
    (let [dialog (save-file-dialog initial-directory initial-file-name)]
      (if (= (.ShowDialog dialog) DialogResult/OK)
        (let [path (.get_FileName dialog)]
          {:status :success :result {:path path}})
        {:status :cancel}))
    (catch Exception e {:status :exception :result (.get_Message e)})))

(defn exists [path _]
  (try
    (File/Exists path)
    (catch Exception e false)))

(declare node)

(defn- directory [path open]
  (try
    (when (Directory/Exists path)
      [{:path path
        :name (-> path DirectoryInfo. .get_Name)}
       (when (contains? open path)
         (node path open))])
    (catch Exception e nil)))

(defn- file [path]
  (try
    {:path path
     :name (Path/GetFileName path)}
    (catch Exception e nil)))

(defn- get-directories [path]
  (try
    (Directory/GetDirectories path)
    (catch Exception e ())))

(defn- realize-directories [directory-paths open]
  (->> directory-paths
       (map #(directory % open))
       (remove nil?)
       (into {})))

(defn- directories [path open]
  (realize-directories (get-directories path) open))

(defn- get-files [path]
  (try
    (Directory/GetFiles path)
    (catch Exception e ())))

(defn- files [path]
  (->> (get-files path)
       (map file)
       (remove nil?)
       (into #{})))

(defn- node [path open]
  {:directories (directories path open)
   :files (files path)})

(defn- directory-exists [path]
  (try
    (when (Directory/Exists path)
      path)
    (catch Exception e nil)))

(defn remove-deleted-directories [open _]
  (->> open
       (map directory-exists)
       (remove nil?)
       (into #{})))

(defn root-directory [path open _]
  (realize-directories [path] open))
