(assembly-load-with-partial-name "System.Windows.Forms")

(ns core.fs
  (:import
   (System.IO
    Directory
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
    DialogResult)))

(defn folder-browser-dialog [folder wrapper]
  (try
    (let [dialog (FolderBrowserDialog.)]
      (if (= (.ShowDialog dialog (.FindForm (:browser wrapper))) DialogResult/OK)
        (assoc folder :path (.SelectedPath dialog))
        (assoc folder :cancel true)))
    (catch Exception e (assoc folder :exception (.get_Message e)))))

(defn- get-files [path]
  (Directory/GetFiles path))

(defn- file-info [path]
  (try
    {:path path
     :name (Path/GetFileName path)
     :extension (Path/GetExtension path)
     :last-write-time (File/GetLastWriteTimeUtc path)}
    (catch Exception e nil)))

(defn reload [reload-file _]
  (let [reloaded-file (file-info (:path reload-file))]
    (if reloaded-file
      reloaded-file
      reload-file)))

(defn get-clj-files [folder _]
  (let [path (:path folder)]
    (if path
      (try
        (assoc folder :files (->> path
                                  get-files
                                  (map file-info)
                                  (remove nil?)
                                  (filter #(= (:extension %) ".clj"))))
        (catch Exception e (assoc folder :exception (.get_Message e))))
      folder)))

(defn read-all-text [open-file _]
  (let [path (:path open-file)]
    (if path
      (try
        (assoc open-file :text (File/ReadAllText path))
        (catch Exception e (assoc open-file :exception (.get_Message e))))
      open-file)))

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

(defn delete [m _]
  (let [path (:path m)]
    (if path
      (try
        (File/Delete path)
        m
        (catch Exception e
          (assoc m :exception (.get_Message e))))
      m)))

(defn save [m _]
  (let [{:keys [path text]} m]
    (if (and path text)
      (try
        (File/WriteAllText path text)
        m
        (catch Exception e
          (assoc m :exception (.get_Message e))))
      m)))

(defn exists [path _]
  (try
    (File/Exists path)
    (catch Exception e false)))
