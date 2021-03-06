(ns dactyl-keyboard.dactyl
  (:refer-clojure :exclude [use import])
  (:require [clojure.core.matrix :refer [array matrix mmul]]
            [clojure.algo.generic.math-functions :refer :all]
            [scad-clj.scad :refer :all]
            [scad-clj.model :refer :all]
            [unicode-math.core :refer :all]))

(defn deg2rad [degrees]
  (* (/ degrees 180) pi))

;;;;;;;;;;;;;;;;;;;;;;
;; Shape parameters ;;
;;;;;;;;;;;;;;;;;;;;;;

(def nrows 5)
(def ncols 6)

(def α (/ π 11))                        ; curvature of the columns
(def β (/ π 36))                        ; curvature of the rows
(def centerrow (- nrows 3))             ; controls front-back tilt
(def centercol 2)                       ; controls left-right tilt / tenting (higher number is more tenting)
(def tenting-angle (/ π 8))            ; or, change this for more precise tenting control
;(def column-style
;  (if (> nrows 5) :orthographic :standard))  ; options include :standard, :orthographic, and :fixed
(def column-style :experiment)

;(defn column-offset [column] (cond
;  (= column 2) [0 2.82 -4.5]
;  (>= column 4) [0 -12 5.64]            ; original [0 -5.8 5.64]
;  :else [0 0 0]))
(defn column-offset [column] (cond
  (= column 0) [0 -2 1.5]
  (= column 2) [0 2.82 -4.5]
  (= column 4) [0 -12 5.64]
  (> column 4) [1 -12 6.64]            ; original [0 -5.8 5.64]
  :else [0 0 0]))

(def thumb-offsets [2 0 -5])
(def thumb-start-col 1)
(def thumbkey-count 3)

(def thumb-length-offset 0)
(def thumb-step-angle 6)
(def thumb-start-angle 60)

(def keyboard-z-offset 20)               ; controls overall height; original=9 with centercol=3; use 16 for centercol=2

(def extra-width 2)                   
(def extra-height 0)               

(def wall-z-offset -10)                 ; length of the first downward-sloping part of the wall (negative)
(def wall-xy-offset 2)                  ; offset in the x and/or y direction for the first downward-sloping part of the wall (negative)
(def wall-thickness 2)                  ; wall thickness parameter; originally 5

;; Settings for column-style == :fixed
;; The defaults roughly match Maltron settings
;;   http://patentimages.storage.googleapis.com/EP0219944A2/imgf0002.png
;; Fixed-z overrides the z portion of the column ofsets above.
;; NOTE: THIS DOESN'T WORK QUITE LIKE I'D HOPED.
(def fixed-angles [(deg2rad 10) (deg2rad 10) 0 0 0 (deg2rad -15) (deg2rad -15)])
(def fixed-x [-41.5 -22.5 0 20.3 41.4 65.5 89.6])  ; relative to the middle finger
(def fixed-z [12.1    8.3 0  5   10.7 14.5 17.5])
(def fixed-tenting (deg2rad 0))

;;;;;;;;;;;;;;;;;;;;;;;
;; General variables ;;
;;;;;;;;;;;;;;;;;;;;;;;

(def lastrow (dec nrows))
(def cornerrow (dec lastrow))
(def lastcol (dec ncols))

;;;;;;;;;;;;;;;;;
;; Switch Hole ;;
;;;;;;;;;;;;;;;;;

(def keyswitch-height 14.4) ;; Was 14.1, then 14.25
(def keyswitch-width 14.4)

(def sa-profile-key-height 12.7)

(def plate-thickness 3)
(def mount-width (+ keyswitch-width 3))
(def mount-height (+ keyswitch-height 3))

(def single-plate
  (let [top-wall (->> (cube (+ keyswitch-width 3) 1.5 plate-thickness)
                      (translate [0
                                  (+ (/ 1.5 2) (/ keyswitch-height 2))
                                  (/ plate-thickness 2)]))
        left-wall (->> (cube 1.5 (+ keyswitch-height 3) plate-thickness)
                       (translate [(+ (/ 1.5 2) (/ keyswitch-width 2))
                                   0
                                   (/ plate-thickness 2)]))
        side-nub (->> (binding [*fn* 30] (cylinder 0.3 10))
                      (rotate (/ π 2) [0 1 0])
                      (translate [0 (+ (/ keyswitch-height 2)) (- plate-thickness 0.3)])
                      (hull (->> 
                                 (translate [0
                                             (+ (/ 1.5 2) (/ keyswitch-height 2))
                                    (/ plate-thickness 2)]))))
         top-wall-extra (->> (cube (/ keyswitch-width 2) 0.4 1)
                      (translate [0
                                  (+ (/ -0.3 2) (/ keyswitch-height 2))
                                  (- plate-thickness (/ 1 2))]))
        ;plate-half (union top-wall left-wall (with-fn 100 side-nub))]
        plate-half (union top-wall left-wall top-wall-extra)]
    (union plate-half
           (->> plate-half
                (mirror [1 0 0])
                (mirror [0 1 0])))))

;;;;;;;;;;;;;;;;
;; SA Keycaps ;;
;;;;;;;;;;;;;;;;

(def sa-length 18.25)
(def sa-double-length 37.5)
(def sa-cap {1 (let [bl2 (/ 18.5 2)
                     m (/ 17 2)
                     key-cap (hull (->> (polygon [[bl2 bl2] [bl2 (- bl2)] [(- bl2) (- bl2)] [(- bl2) bl2]])
                                        (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                        (translate [0 0 0.05]))
                                   (->> (polygon [[m m] [m (- m)] [(- m) (- m)] [(- m) m]])
                                        (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                        (translate [0 0 6]))
                                   (->> (polygon [[6 6] [6 -6] [-6 -6] [-6 6]])
                                        (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                        (translate [0 0 12])))]
                 (->> key-cap
                      (translate [0 0 (+ 5 plate-thickness)])
                      (color [220/255 163/255 163/255 1])))
             2 (let [bl2 (/ sa-double-length 2)
                     bw2 (/ 18.25 2)
                     key-cap (hull (->> (polygon [[bw2 bl2] [bw2 (- bl2)] [(- bw2) (- bl2)] [(- bw2) bl2]])
                                        (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                        (translate [0 0 0.05]))
                                   (->> (polygon [[6 16] [6 -16] [-6 -16] [-6 16]])
                                        (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                        (translate [0 0 12])))]
                 (->> key-cap
                      (translate [0 0 (+ 5 plate-thickness)])
                      (color [127/255 159/255 127/255 1])))
             1.5 (let [bl2 (/ 18.25 2)
                       bw2 (/ 28 2)
                       key-cap (hull (->> (polygon [[bw2 bl2] [bw2 (- bl2)] [(- bw2) (- bl2)] [(- bw2) bl2]])
                                          (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                          (translate [0 0 0.05]))
                                     (->> (polygon [[11 6] [-11 6] [-11 -6] [11 -6]])
                                          (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                          (translate [0 0 12])))]
                   (->> key-cap
                        (translate [0 0 (+ 5 plate-thickness)])
                        (color [240/255 223/255 175/255 1])))})

;;;;;;;;;;;;;;;;;;;;;;;;;
;; Placement Functions ;;
;;;;;;;;;;;;;;;;;;;;;;;;;

(def columns (range 0 ncols))
(def rows (range 0 nrows))

(def cap-top-height (+ plate-thickness sa-profile-key-height -2))
(def row-radius (+ (/ (/ (+ mount-height extra-height) 2)
                      (Math/sin (/ α 2)))
                   cap-top-height))
(defn row-radius-angle [a] (+ (/ (/ (+ mount-height extra-height) 2)
                      (Math/sin (/ a 2)))
                   cap-top-height))
(def column-radius (+ (/ (/ (+ mount-width extra-width) 2)
                         (Math/sin (/ β 2)))
                      cap-top-height))
(def column-x-delta (+ -1 (- (* column-radius (Math/sin β)))))
(def column-base-angle (* β (- centercol 2)))

(defn apply-key-geometry [translate-fn rotate-x-fn rotate-y-fn column row shape]
  (let [column-angle (* β (- centercol column))
        placed-shape (->> shape
                          (translate-fn [0 0 (- row-radius)])
                          (rotate-x-fn  (* α (- centerrow row)))
                          (translate-fn [0 0 row-radius])
                          (translate-fn [0 0 (- column-radius)])
                          (rotate-y-fn  column-angle)
                          (translate-fn [0 0 column-radius])
                          (translate-fn (column-offset column)))
        column-z-delta (* column-radius (- 1 (Math/cos column-angle)))
        placed-shape-ortho (->> shape
                                (translate-fn [0 0 (- row-radius)])
                                (rotate-x-fn  (* α (- centerrow row)))
                                (translate-fn [0 0 row-radius])
                                (rotate-y-fn  column-angle)
                                (translate-fn [(- (* (- column centercol) column-x-delta)) 0 column-z-delta])
                                (translate-fn (column-offset column)))
        placed-shape-fixed (->> shape
                                (rotate-y-fn  (nth fixed-angles column))
                                (translate-fn [(nth fixed-x column) 0 (nth fixed-z column)])
                                (translate-fn [0 0 (- (+ row-radius (nth fixed-z column)))])
                                (rotate-x-fn  (* α (- centerrow row)))
                                (translate-fn [0 0 (+ row-radius (nth fixed-z column))])
                                (rotate-y-fn  fixed-tenting)
                                (translate-fn [0 (second (column-offset column)) 0])
                                )
        place-experiment (let [
                               [cox coy coz] (column-offset column)
                               orr (row-radius-angle α)
                               F (/ orr (- orr coz))
                               na (Math/atan (* (Math/tan α) F))
                               row-radius (row-radius-angle na)
                               ]
                           (->> shape
                          (translate-fn [0 0 (- row-radius)])
                          (rotate-x-fn  (* na (- centerrow row)))
                          (translate-fn [0 0 row-radius])
                          (translate-fn [0 0 (- column-radius)])
                          (rotate-y-fn  column-angle)
                          (translate-fn [0 0 column-radius])
                          (translate-fn [cox coy coz])))
        ]
    (->> (case column-style
           :orthographic placed-shape-ortho
           :fixed        placed-shape-fixed
           :experiment place-experiment
           placed-shape)
         (rotate-y-fn  tenting-angle)
         (translate-fn [0 0 keyboard-z-offset]))))

(defn key-place [column row shape]
  (apply-key-geometry translate
    (fn [angle obj] (rotate angle [1 0 0] obj))
    (fn [angle obj] (rotate angle [0 1 0] obj))
    column row shape))

(defn rotate-around-x [angle position]
  (mmul
   [[1 0 0]
    [0 (Math/cos angle) (- (Math/sin angle))]
    [0 (Math/sin angle)    (Math/cos angle)]]
   position))

(defn rotate-around-y [angle position]
  (mmul
   [[(Math/cos angle)     0 (Math/sin angle)]
    [0                    1 0]
    [(- (Math/sin angle)) 0 (Math/cos angle)]]
   position))

(defn key-position [column row position]
  (apply-key-geometry (partial map +) rotate-around-x rotate-around-y column row position))

(defn keycond [row column]
  (not (and 
         (== row (dec nrows))
         (or 
           (< column thumb-start-col)
           (> column 3)))))

(def key-holes
  (apply union
         (for [column columns
               row rows
               :when (keycond row column)]
           (->> single-plate
                (key-place column row)))))

(def caps
  (apply union
         (for [column columns
               row rows
               :when (keycond row column)]
           (->> (sa-cap (if (= column 5) 1 1))
                (key-place column row)))))

; (pr (rotate-around-y π [10 0 1]))
; (pr (key-position 1 cornerrow [(/ mount-width 2) (- (/ mount-height 2)) 0]))

;;;;;;;;;;;;;;;;;;;;
;; Wall funcs     ;;
;;;;;;;;;;;;;;;;;;;;


(defn bottom [height p]
  (->> (project p)
       (extrude-linear {:height height :twist 0 :convexity 0})
       (translate [0 0 (- (/ height 2) 10)])))

(defn bottom-hull [& p]
  (hull p (bottom 0.001 p)))

(def left-wall-x-offset 10)
(def left-wall-z-offset  3)

(defn left-key-position [row direction]
  (map - (key-position 0 row [(* mount-width -0.5) (* direction mount-height 0.5) 0]) [left-wall-x-offset 0 left-wall-z-offset]) )

(defn left-key-place [row direction shape]
  (key-place 0 row shape))

(defn wall-locate1 [dx dy] [(* dx wall-thickness) (* dy wall-thickness) -1])
(defn wall-locate2 [dx dy] [(* dx wall-xy-offset) (* dy wall-xy-offset) wall-z-offset])
(defn wall-locate3 [dx dy] [(* dx (+ wall-xy-offset wall-thickness)) (* dy (+ wall-xy-offset wall-thickness)) wall-z-offset])
(defn wall-locate4 [dx dy] [(* dx (+ wall-xy-offset (/ mount-width 2))) (* dy (+ wall-xy-offset (/ mount-height 2))) wall-z-offset])

(defn wall-brace [place1 dx1 dy1 post1 place2 dx2 dy2 post2]
  (union
    (hull
      (place1 post1)
      (place1 (translate (wall-locate1 dx1 dy1) post1))
      (place1 (translate (wall-locate2 dx1 dy1) post1))
      (place1 (translate (wall-locate3 dx1 dy1) post1))
      (place2 post2)
      (place2 (translate (wall-locate1 dx2 dy2) post2))
      (place2 (translate (wall-locate2 dx2 dy2) post2))
      (place2 (translate (wall-locate3 dx2 dy2) post2)))
    (bottom-hull
      (place1 (translate (wall-locate2 dx1 dy1) post1))
      (place1 (translate (wall-locate3 dx1 dy1) post1))
      (place2 (translate (wall-locate2 dx2 dy2) post2))
      (place2 (translate (wall-locate3 dx2 dy2) post2)))
      ))

(defn key-wall-brace [x1 y1 dx1 dy1 post1 x2 y2 dx2 dy2 post2]
  (wall-brace (partial key-place x1 y1) dx1 dy1 post1
              (partial key-place x2 y2) dx2 dy2 post2))

;;;;;;;;;;;;;;;;;;;;
;; Web Connectors ;;
;;;;;;;;;;;;;;;;;;;;

(def web-thickness 3)
(def post-size 0.1)
(def web-post (->> (cube post-size post-size web-thickness)
                   (translate [0 0 (+ (/ web-thickness -2)
                                      plate-thickness)])))

(def post-adj (/ post-size 2))
(def web-post-tr (translate [(- (/ mount-width 2) post-adj) (- (/ mount-height 2) post-adj) 0] web-post))
(def web-post-tl (translate [(+ (/ mount-width -2) post-adj) (- (/ mount-height 2) post-adj) 0] web-post))
(def web-post-bl (translate [(+ (/ mount-width -2) post-adj) (+ (/ mount-height -2) post-adj) 0] web-post))
(def web-post-br (translate [(- (/ mount-width 2) post-adj) (+ (/ mount-height -2) post-adj) 0] web-post))

(defn triangle-hulls [& shapes]
  (apply union
         (map (partial apply hull)
              (partition 3 1 shapes))))

(def connectors
  (apply union
         (concat
          ;; Row connections
          (for [column (range 0 (dec ncols))
                row (range 0 nrows)
                :when (and
                        (keycond row (+ column 1))
                        (not (and 
                               (< column thumb-start-col)
                               (== row (dec nrows))
                               )))
                ]
            (triangle-hulls
             (key-place (inc column) row web-post-tl)
             (key-place column row web-post-tr)
             (key-place (inc column) row web-post-bl)
             (key-place column row web-post-br)))

          ;; Column connections
          (for [column columns
                row (range 0 (dec nrows))
                :when (keycond (inc row) column)]
            (triangle-hulls
             (key-place column row web-post-bl)
             (key-place column row web-post-br)
             (key-place column (inc row) web-post-tl)
             (key-place column (inc row) web-post-tr)))

          ;; Diagonal connections
          (for [column (range 0 (dec ncols))
                row (range 0 (dec nrows))
                :when (keycond (inc row) column)]
            (triangle-hulls
             (key-place column row web-post-br)
             (key-place column (inc row) web-post-tr)
             (key-place (inc column) row web-post-bl)
             (key-place (inc column) (inc row) web-post-tl)))

          [
            (triangle-hulls
             (key-place 3 4 web-post-tr)
             (key-place 3 4 web-post-br)
             (key-place 4 3 web-post-bl)
             )
           ]
          )))

(def key-holes-space
  (apply union
         (for [column columns
               row rows
               :when (keycond row column)]
           (->> 
            (triangle-hulls
             (key-place column row web-post-tl)
             (key-place column row web-post-tr)
             (key-place column row web-post-bl)
             (key-place column row web-post-br))))))

;;;;;;;;;;;;
;; Thumbs ;;
;;;;;;;;;;;;

(def thumb-length 
  (+ (* mount-width 1.5)
     (/ (+ mount-height extra-height) (Math/sin (deg2rad thumb-step-angle)))
     )
  )

(def thumborigin
  (map + (key-position (dec thumb-start-col) lastrow [0 0 0])
         thumb-offsets))

(defn thumborigintransform [shape]
  (->> shape
       ;(rotate (deg2rad 10) [0 0 1])
       (translate [(* thumb-length (sin (deg2rad thumb-start-angle))) (* (* thumb-length (cos (deg2rad thumb-start-angle))) -1) 0])
       (rotate (deg2rad -35) [0 1 0])
       (rotate (deg2rad 45) [1 0 0])
       (translate [0 -5 -2])
       (translate thumborigin)
       ))

(defn thumb-n-place [n, shape]
  (->> shape
       (rotate (deg2rad -5) [0 1 0])
       (translate [0 (- thumb-length thumb-length-offset) 0])
       (rotate (deg2rad (+ (* n thumb-step-angle) thumb-start-angle)) [0 0 1])
       thumborigintransform
       ))

(def thumb-tr-place (partial thumb-n-place 0))
(def thumb-tl-place (partial thumb-n-place 1))
(def thumb-mr-place (partial thumb-n-place 2))
(def thumb-ml-place (partial thumb-n-place 3))
(def thumb-br-place (partial thumb-n-place 4))
(def thumb-bl-place (partial thumb-n-place 5))

(defn thumb-1x-layout [shape]
  (union
   (thumb-n-place 0 shape)
   (thumb-n-place 1 shape)
   (thumb-n-place 2 shape)
   ))


(def larger-plate
  (let [plate-height (/ (- sa-double-length mount-height) 3)
        top-plate (->> (cube mount-width plate-height web-thickness)
                       (translate [0 (/ (+ plate-height mount-height) 2)
                                   (- plate-thickness (/ web-thickness 2))]))
        ]
    (union top-plate (mirror [0 1 0] top-plate))))

;(def thumbcaps
; (union
;  (thumb-1x-layout (sa-cap 1))
;  (thumb-15x-layout (rotate (/ π 2) [0 0 1] (sa-cap 1.5)))))

(def thumbcaps ())

(def thumb
  (union
   (thumb-1x-layout (rotate (deg2rad 90) [0 0 1] single-plate))
   (thumb-1x-layout larger-plate)
   ))

(def thumb-post-tr (translate [(- (/ mount-width 2) post-adj)  (- (/ mount-height  1.15) post-adj) 0] web-post))
(def thumb-post-tl (translate [(+ (/ mount-width -2) post-adj) (- (/ mount-height  1.15) post-adj) 0] web-post))
(def thumb-post-bl (translate [(+ (/ mount-width -2) post-adj) (+ (/ mount-height -1.15) post-adj) 0] web-post))
(def thumb-post-br (translate [(- (/ mount-width 2) post-adj)  (+ (/ mount-height -1.15) post-adj) 0] web-post))

(def thumb-key-space
  (apply union
         (for [x (range 0 thumbkey-count)]
           (->> 
            (triangle-hulls
             (thumb-n-place x thumb-post-tl)
             (thumb-n-place x thumb-post-tr)
             (thumb-n-place x thumb-post-bl)
             (thumb-n-place x thumb-post-br))))))

(def thumb-connectors 
  (
   union
   (for [x (range 0 thumb-start-col)]
     (union 
      (triangle-hulls    
             ((partial thumb-n-place (dec (- thumb-start-col x))) thumb-post-tl)
             ((partial key-place x cornerrow) web-post-bl)
             ((partial thumb-n-place (dec (- thumb-start-col x))) thumb-post-tr)
             ((partial key-place x cornerrow) web-post-br)
       ))
   )
   (for [x (range 0 (dec thumb-start-col))]
     (union 
      (triangle-hulls    
             ((partial key-place x cornerrow) web-post-br)
             ((partial thumb-n-place (dec (- thumb-start-col x))) thumb-post-tr)
             ((partial key-place (inc x) cornerrow) web-post-bl)
             ((partial thumb-n-place (- (- thumb-start-col x) 2)) thumb-post-tl)
       ))
   )
   (for [x (range 0 (dec thumbkey-count))]
     (union 
      (triangle-hulls    
             ((partial thumb-n-place x) thumb-post-tl)
             ((partial thumb-n-place x) thumb-post-bl)
             ((partial thumb-n-place (inc x)) thumb-post-tr)
             ((partial thumb-n-place (inc x)) thumb-post-br)
       ))
   )

   (triangle-hulls    
     ((partial key-place 0 cornerrow) web-post-bl)
     ((partial thumb-n-place 1) thumb-post-tr)
     ((partial thumb-n-place 0) thumb-post-tl)
   )

   (triangle-hulls    
     ((partial key-place thumb-start-col cornerrow) web-post-bl)
     ((partial key-place (dec thumb-start-col) cornerrow) web-post-br)
     ((partial key-place thumb-start-col lastrow) web-post-tl)
     ((partial thumb-n-place 0) thumb-post-tr)
     ((partial key-place thumb-start-col lastrow) web-post-bl)
     ((partial thumb-n-place 0) thumb-post-br)
     ((partial key-place thumb-start-col lastrow) web-post-br)
   )
))

(def thumb-wall
  (union
   ; thumb walls

   (for [x (range thumb-start-col thumbkey-count)] 
     (union 
       (wall-brace (partial thumb-n-place x)  0 1 thumb-post-tl (partial thumb-n-place x)  0 1 thumb-post-tr)
       ))

   (for [x (range 0 thumbkey-count)] 
     (union 
       (wall-brace (partial thumb-n-place x)  0 -1 thumb-post-bl (partial thumb-n-place x)  0 -1 thumb-post-br)
       ))

   (for [x (range thumb-start-col (dec thumbkey-count))] 
     (union 
       (wall-brace (partial thumb-n-place (inc x))  0 1 thumb-post-tr (partial thumb-n-place x)  0 1 thumb-post-tl)
       ))

   (for [x (range 0 (dec thumbkey-count))] 
     (union 
       (wall-brace (partial thumb-n-place (inc x))  0 -1 thumb-post-br (partial thumb-n-place x)  0 -1 thumb-post-bl)
       ))

   ;bottom
   (wall-brace (partial thumb-n-place (dec thumbkey-count))  0 1 thumb-post-tl (partial thumb-n-place (dec thumbkey-count)) -1 0 thumb-post-tl)
   (wall-brace (partial thumb-n-place (dec thumbkey-count))  0 -1 thumb-post-bl (partial thumb-n-place (dec thumbkey-count)) -1 0 thumb-post-bl)
   (wall-brace (partial thumb-n-place (dec thumbkey-count))  -1 0 thumb-post-tl (partial thumb-n-place (dec thumbkey-count)) -1 0 thumb-post-bl)

   ;connection to main
   (wall-brace (partial key-place (+ thumb-start-col 0) lastrow) 0 -1 web-post-br (partial thumb-n-place 0) 0 -1 thumb-post-br)
   (wall-brace (partial thumb-n-place thumb-start-col) 0 1 thumb-post-tr (partial left-key-place cornerrow -1) -1 0 web-post-bl)
   ))


;;;;;;;;;;
;; Case ;;
;;;;;;;;;;

(def case-walls
  (union
   ; back wall
   (for [x (range 0 ncols)] (key-wall-brace x 0 0 1 web-post-tl x       0 0 1 web-post-tr))
   (for [x (range 1 ncols)] (key-wall-brace x 0 0 1 web-post-tl (dec x) 0 0 1 web-post-tr))
   (key-wall-brace lastcol 0 0 1 web-post-tr lastcol 0 1 0 web-post-tr)
   ; right wall
   (for [y (range 0 lastrow)] (key-wall-brace lastcol y 1 0 web-post-tr lastcol y       1 0 web-post-br))
   (for [y (range 1 lastrow)] (key-wall-brace lastcol (dec y) 1 0 web-post-br lastcol y 1 0 web-post-tr))
   (key-wall-brace lastcol cornerrow 0 -1 web-post-br lastcol cornerrow 1 0 web-post-br)
   ; left wall
   (for [y (range 0 lastrow)] (union (wall-brace (partial left-key-place y 1)       -1 0 web-post-tl (partial left-key-place y -1) -1 0 web-post-bl)
                                     ))
   (for [y (range 1 lastrow)] (union (wall-brace (partial left-key-place (dec y) -1) -1 0 web-post-bl (partial left-key-place y  1) -1 0 web-post-tl)
                                     ))

   (wall-brace (partial key-place 0 0) 0 1 web-post-tl (partial key-place 0 0) -1 0 web-post-tl)
   ; front wall
   (key-wall-brace lastcol 0 0 1 web-post-tr lastcol 0 1 0 web-post-tr)
   (for [x (range thumb-start-col 3)] 
     (union 
       (key-wall-brace (+ x 0) lastrow 0 -1 web-post-br (+ x 1) lastrow 0 -1 web-post-bl)
       (key-wall-brace (+ x 1) lastrow  0 -1 web-post-bl (+ x 1) lastrow 0 -1 web-post-br)
       ))

   ;(key-wall-brace 3 lastrow   0 -1 web-post-bl 3 lastrow 0.5 -1 web-post-br)
   (key-wall-brace 3 lastrow 0 -1 web-post-br 4 cornerrow 1 -1 web-post-bl)
   (for [x (range 4 ncols)] (key-wall-brace x cornerrow 0 -1 web-post-bl x       cornerrow 0 -1 web-post-br))
   (for [x (range 5 ncols)] (key-wall-brace x cornerrow 0 -1 web-post-bl (dec x) cornerrow 0 -1 web-post-br))

   thumb-wall
 ))

(def board-wall-thickness 1.4)
(defn board-space 
  ([board-space-size port-size] (board-space board-space-size port-size [0 0 0 1] (translate [-4 6 0])))
  ([board-space-size port-size reference-key transform-fn] (board-space board-space-size port-size reference-key transform-fn cube))
  ([board-space-size port-size reference-key transform-fn port-fn]
   (let [[bw bl bh] board-space-size
         [pw pl ph] port-size
         ;epw (+ pw bw)
         epw pw
         port-inset 5
         [rc rr wx wy] reference-key
         board-position (key-position rc rr (wall-locate4 wx wy))
         ]
     (union
       (->> 
         (union
           (apply cube board-space-size)
           (->>
             (port-fn epw pl ph)
             ;jtranslate [(+ (/ bw -2) (/ epw 2)) (+ (/ bl 2) (/ pl 2) (- port-inset))])
             (translate [(+ (/ bw 2) (/ epw 2)) (+ (/ bl 2) (/ pl 2) (- port-inset))])
             )
           )
         (translate [0 (/ bl -2) (+ (/ bh 2) board-wall-thickness)])
         transform-fn
         (translate (assoc (vec board-position) 2 0))
         )
       (->> 
         (union
           (apply cube board-space-size)
           (->>
             (port-fn epw pl ph)
             ;(translate [(+ (/ bw -2) (/ epw 2)) (+ (/ bl 2) (/ pl 2) (- port-inset))])
             (translate [(+ (/ bw 2) (/ epw 2)) (+ (/ bl 2) (/ pl 2) (- port-inset))])
             )
           )
         (translate [0 (/ bl -2) (+ (/ bh 2) board-wall-thickness)])
         (rotate (deg2rad 20) [0 0 1]) ; for easier insert
         transform-fn
         (translate (assoc (vec board-position) 2 0))
         )
       )))
)

(defn board-holder 
  ([board-space-size port-size] (board-holder board-space-size port-size [0 0 0 1] (translate [-4 6 0])))
  ([board-space-size port-size reference-key transform-fn] (board-holder board-space-size port-size reference-key transform-fn cube))
  ([board-space-size port-size reference-key transform-fn port-fn]
  (let [[bw bl bh] board-space-size
        [rc rr wx wy] reference-key
        board-position (key-position rc rr (wall-locate4 wx wy))
        ]
  (union
    (difference
      (->> 
        (difference
          (union
            (translate [0 0 (/ board-wall-thickness -2)]
                       (color [0 0 1]
                              (apply cube (map + [(* board-wall-thickness 2) (* board-wall-thickness 2) board-wall-thickness] board-space-size))))
            )
          (->>
            (apply cube [board-wall-thickness (- bl (/ board-wall-thickness 2)) bh])
            (color [1 1 0])
            (translate [(+ (/ bw 2) (/ board-wall-thickness 2)) 0 0])
            )
          (->>
            (apply cube [board-wall-thickness bl (/ bh 3)])
            (color [0 1 1])
            (translate [(+ (/ bw 2) (/ board-wall-thickness 2)) 0 0])
            (translate [0 0 (/ bh 3)])
            )
          (->>
            (apply cube [board-wall-thickness bl (/ bh 3)])
            (color [0 1 1])
            (translate [(+ (/ bw 2) (/ board-wall-thickness 2)) 0 0])
            (translate [0 0 (/ bh -3)])
            )
          )
        (translate [0 (/ bl -2) (+ (/ bh 2) board-wall-thickness)])
        transform-fn
        (translate (assoc (vec board-position) 2 0))
        )
      (board-space board-space-size port-size reference-key transform-fn port-fn)
      )
    )))
  )


(defn screw-insert-shape [bottom-radius top-radius height]
   (union 
     (binding [*fn* 30]
     (cylinder [bottom-radius top-radius] (+ height 3)))
          ;(translate [0 0 (/ height 2)] (sphere top-radius)) it broke
          ))


(defn screw-insert [column row bottom-radius top-radius height]
  (let [shift-right   (= column lastcol)
        shift-left    (= column 0)
        shift-up      (and (not (or shift-right shift-left)) (= row 0))
        shift-down    (and (not (or shift-right shift-left)) (>= row lastrow))
        position      (if shift-up     (key-position column row (wall-locate4  0  1))
                        (if shift-down  (key-position column row (wall-locate4  0 -1))
                          (if shift-left (key-position column row (wall-locate4 -1 0))
                            (key-position column row (wall-locate4  1  0)))))
        ]
    (->> (screw-insert-shape bottom-radius top-radius height)
         (translate [(first position) (second position) (/ height 2)])
         )))

(defn screw-insert-all-shapes [bottom-radius top-radius height]
  (union (screw-insert 1 0         bottom-radius top-radius height)
         (screw-insert 0 (- lastrow 1)   bottom-radius top-radius height)
         (screw-insert 3 (+ lastrow 0)  bottom-radius top-radius height)
         (screw-insert 3 0         bottom-radius top-radius height)
         (screw-insert lastcol 1   bottom-radius top-radius height)
         ))
(def screw-insert-height 6.2)
(def screw-insert-bottom-radius (/ 5.31 2))
(def screw-insert-top-radius (/ 5.1 2))
(def screw-insert-holes  (screw-insert-all-shapes screw-insert-bottom-radius screw-insert-top-radius screw-insert-height))
(def screw-insert-outers (screw-insert-all-shapes (+ screw-insert-bottom-radius 1.6) (+ screw-insert-top-radius 1.6) (+ screw-insert-height 1.5)))
(def screw-insert-screw-holes  (screw-insert-all-shapes 1.7 1.7 350))

(def wire-post-height 7)
(def wire-post-overhang 3.5)
(def wire-post-diameter 2.6)
(defn wire-post [direction offset]
   (->> (union (translate [0 (* wire-post-diameter -0.5 direction) 0] (cube wire-post-diameter wire-post-diameter wire-post-height))
               (translate [0 (* wire-post-overhang -0.5 direction) (/ wire-post-height -2)] (cube wire-post-diameter wire-post-overhang wire-post-diameter)))
        (translate [0 (- offset) (+ (/ wire-post-height -2) 3) ])
        (rotate (/ α -2) [1 0 0])
        (translate [3 (/ mount-height -2) 0])))

(def wire-posts
  (union
     (thumb-ml-place (translate [-5 0 -2] (wire-post  1 0)))
     (thumb-ml-place (translate [ 0 0 -2.5] (wire-post -1 6)))
     (thumb-ml-place (translate [ 5 0 -2] (wire-post  1 0)))
     (for [column (range 0 lastcol)
           row (range 0 cornerrow)]
       (union
        (key-place column row (translate [-5 0 0] (wire-post 1 0)))
        (key-place column row (translate [0 0 0] (wire-post -1 6)))
        (key-place column row (translate [5 0 0] (wire-post  1 0)))))))

(def micro-usb-size [3.2 16 9])
(def pro-micro-space-size [3 34 19.3])
(def pro-micro [pro-micro-space-size micro-usb-size [0 0 -0.8 1] (partial translate [0 0 0])])

(def mini-usb-size [4.2 16 8.5])
(def teensy-space-size [3.5 52 19])
(def teensy [teensy-space-size mini-usb-size [0 0 -0.8 1] (partial translate [0 0 0])])

(def pro-micro-mini-space-size [2 40.5 23.5])
(def pro-micro-mini [pro-micro-mini-space-size mini-usb-size [0 0 -0.8 1] (partial translate [0 0 0])])

(def trrs-port-size [6 16 7])
(def trrs-space-size [2.5 13 26])
(def trrs [trrs-space-size trrs-port-size [0 2 -1 0] 
           (fn [it] (->> it
                         (rotate (deg2rad 90) [0 0 1])
                         (mirror [0 1 0])
                         )) 
           ])
(def trrs2 [trrs-space-size trrs-port-size [0 2 -1 0] 
           (fn [it] (->> it
                         (rotate (deg2rad 90) [0 0 1])
                         )) 
           ])

(def mcp-io-board [[3.5 37.5 20.5] [0 0 0] [0 0 -1 1] 
           (fn [it] (->> it
                         (rotate (deg2rad -18) [0 0 1])
                         (translate [4 -2 0])
                         )) 
           ])

(defn model-right [boards] 
  (difference
    (union
      key-holes
      connectors
      thumb
      thumb-connectors
      (apply union (for [b boards] (apply board-holder b)))
      case-walls
      screw-insert-outers
      )
    (apply union (for [b boards] (apply board-space b)))
    screw-insert-holes
    (translate [0 0 -100] (cube 200 200 200)) ; the ground
    ))

(spit "things/right.scad"
      (write-scad (model-right [trrs pro-micro])))

(spit "things/right-port.scad"
      (write-scad (intersection 
                    (model-right [trrs pro-micro])
                    (translate [-120 30 4] (cube 200 100 50))
                             )))


(spit "things/right-thumb.scad"
      (write-scad (intersection 
                    (model-right [trrs])
                    (translate [-100 -55 20] (cube 200 70 100))
                             )))

(spit "things/left.scad"
      (write-scad (mirror [-1 0 0] (model-right [trrs2 pro-micro]))))

(spit "things/left-port.scad"
      (write-scad (intersection 
                    (mirror [-1 0 0] (model-right [trrs2 mcp-io-board]))
                    (translate [120 25 4] (cube 200 80 50))
                             )))

(spit "things/right-plate.scad"
      (write-scad
        (extrude-linear {:height 2}
        (projection 2
                     (translate [0 0 -0.1]
                       (difference (
                                    union 
                                    case-walls
                                    key-holes-space
                                    connectors
                                    thumb-key-space
                                    thumb-connectors
                                    screw-insert-outers
                                    )
                                   (translate [0 0 -10] screw-insert-screw-holes))
                       )))
      ))


(defn -main [] 1)  ; dummy to make it easier to batch
